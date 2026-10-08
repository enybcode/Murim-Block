'use strict';

const fs = require('node:fs');
const path = require('node:path');
const BEGIN = '# BEGIN MURIMBLOCK CLOUD PROXY';
const END = '# END MURIMBLOCK CLOUD PROXY';

function property(value) {
  return String(value).replace(/[\\\s:=#!]|[^\x20-\x7e]/g, char => {
    if (char === '\n') return '\\n';
    if (char === '\r') return '\\r';
    if (char === '\t') return '\\t';
    if (char.charCodeAt(0) > 126) return '\\u' + char.charCodeAt(0).toString(16).padStart(4, '0');
    return '\\' + char;
  });
}

function proxy(raw) {
  if (!raw) return null;
  let url;
  try { url = new URL(raw); } catch { throw new Error('Invalid proxy URL; its value is not logged.'); }
  if (url.protocol !== 'http:' || !url.hostname || url.username || url.password || url.search || url.hash
      || (url.pathname && url.pathname !== '/')) {
    throw new Error('Use the managed HTTP proxy, without URL credentials or a path. TLS destinations still use HTTPS.');
  }
  return { host: url.hostname, port: url.port || '80' };
}

function nonProxyHosts(raw = '') {
  const hosts = new Set(['localhost', '127.*', '[::1]']);
  for (let host of raw.split(',').map(value => value.trim()).filter(Boolean)) {
    // Java cannot express CIDR ranges. Keep those destinations on the managed proxy.
    if (host.includes('/')) continue;
    host = host.replace(/^(\[[^\]]+\]|[^:]+):\d+$/, '$1');
    if (host === '*') hosts.add('*');
    else if (host.startsWith('.')) { hosts.add(host.slice(1)); hosts.add('*' + host); }
    else { hosts.add(host); if (!host.includes('*') && /^[a-z]/i.test(host)) hosts.add('*.' + host); }
  }
  return [...hosts].join('|');
}

function configure(existing, env) {
  const begin = existing.indexOf(BEGIN);
  const end = existing.indexOf(END);
  if ((begin === -1) !== (end === -1) || (begin !== -1 && end < begin)
      || (begin !== -1 && existing.indexOf(BEGIN, begin + BEGIN.length) !== -1)
      || (end !== -1 && existing.indexOf(END, end + END.length) !== -1)) {
    throw new Error('Malformed managed proxy block; existing properties have not been changed.');
  }
  const preserved = begin === -1 ? existing : existing.slice(0, begin) + existing.slice(end + END.length).replace(/^\r?\n/, '');
  const https = proxy(env.HTTPS_PROXY || env.https_proxy || env.HTTP_PROXY || env.http_proxy);
  const http = proxy(env.HTTP_PROXY || env.http_proxy || env.HTTPS_PROXY || env.https_proxy);
  const lines = [];
  for (const [protocol, config] of [['http', http], ['https', https]]) {
    if (!config) continue;
    lines.push(`systemProp.${protocol}.proxyHost=${property(config.host)}`);
    lines.push(`systemProp.${protocol}.proxyPort=${property(config.port)}`);
    lines.push(`systemProp.${protocol}.nonProxyHosts=${property(nonProxyHosts(env.NO_PROXY || env.no_proxy))}`);
  }
  if (!lines.length) return preserved;
  return preserved.replace(/\n*$/, '\n') + BEGIN + '\n' + lines.join('\n') + '\n' + END + '\n';
}

if (require.main === module) {
  try {
    if (!process.argv[2]) throw new Error('Pass the writable GRADLE_USER_HOME directory.');
    const directory = path.resolve(process.argv[2]);
    fs.mkdirSync(directory, { recursive: true });
    const destination = path.join(directory, 'gradle.properties');
    const existing = fs.existsSync(destination) ? fs.readFileSync(destination, 'utf8') : '';
    const updated = configure(existing, process.env);
    if (updated !== existing) fs.writeFileSync(destination, updated, { mode: 0o600 });
    console.log('Cloud Gradle proxy configured; inherited CA trust and unrelated properties preserved.');
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}

module.exports = { configure, nonProxyHosts, property, proxy };
