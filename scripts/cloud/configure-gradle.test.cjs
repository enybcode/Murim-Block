'use strict';

const assert = require('node:assert/strict');
const { test } = require('node:test');
const { configure, nonProxyHosts, property, proxy } = require('./configure-gradle.cjs');

test('managed proxy is applied to HTTP and HTTPS, including the wrapper', () => {
  const result = configure('', { HTTPS_PROXY: 'http://proxy:8080' });
  assert.match(result, /systemProp.https.proxyHost=proxy/);
  assert.match(result, /systemProp.http.proxyPort=8080/);
});

test('configuration is idempotent and preserves unrelated JVM and TLS properties', () => {
  const original = '# local settings\norg.gradle.jvmargs=-Xmx2G\nsystemProp.javax.net.ssl.trustStore=/managed/ca.jks\n';
  const env = { HTTPS_PROXY: 'http://proxy:8080' };
  const result = configure(original, env);
  assert.ok(result.startsWith(original));
  assert.equal(configure(result, env), result);
});

test('proxy rotation replaces only the managed block', () => {
  const original = 'org.gradle.caching=true\n';
  const first = configure(original, { HTTPS_PROXY: 'http://first:8080' });
  const next = configure(first, { HTTPS_PROXY: 'http://second:3128' });
  assert.match(next, /https.proxyHost=second/);
  assert.doesNotMatch(next, /first/);
  assert.equal(configure(next, {}), original);
});

test('explicit separate and lowercase proxies are respected', () => {
  const result = configure('', { http_proxy: 'http://plain', https_proxy: 'http://secure:8080' });
  assert.match(result, /http.proxyHost=plain/);
  assert.match(result, /http.proxyPort=80/);
  assert.match(result, /https.proxyHost=secure/);
});

test('bad proxy URLs do not expose credentials in errors', () => {
  for (const value of ['broken', 'https://proxy:8080', 'http://user:secret@proxy:8080', 'http://proxy/path']) {
    assert.throws(() => proxy(value), error => !error.message.includes('secret') && !error.message.includes(value));
  }
});

test('property values are escaped instead of injecting new properties', () => {
  assert.equal(property('one\ntwo=three'), 'one\\ntwo\\=three');
  assert.equal(property('C:\\cache'), 'C\\:\\\\cache');
});

test('NO_PROXY preserves local hosts and suffixes without bypassing the proxy for CIDR', () => {
  const result = nonProxyHosts('.internal.example,localhost:1234,10.0.0.0/8');
  assert.ok(result.includes('localhost|127.*|[::1]'));
  assert.ok(result.includes('internal.example|*.internal.example'));
  assert.ok(!result.includes('10.'));
});

test('malformed ownership markers leave the original configuration untouched', () => {
  assert.throws(() => configure('# BEGIN MURIMBLOCK CLOUD PROXY\n', {}), /Malformed/);
});
