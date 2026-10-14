/**
 * Crée le Google Form « Propose ton art martial » (Murimblock) dans ton Google Drive.
 *
 * 1. Va sur https://script.google.com et clique sur « Nouveau projet ».
 * 2. Efface tout, colle ce fichier entier, puis enregistre (Ctrl+S).
 * 3. Clique sur « Exécuter » (fonction creerFormulaire) et accepte l'autorisation Google Forms.
 * 4. Ouvre « Journal d'exécution » en bas : il donne le lien à partager et le lien pour modifier le formulaire.
 */
function creerFormulaire() {
  var form = FormApp.create('Murimblock · Propose ton art martial');
  form.setDescription(
    "Invente un art martial pour Murimblock : une arme, un passif et 3 techniques. " +
    "Pas besoin de chiffres ni de détails, quelques phrases suffisent. Enzo s'occupe du reste.");
  form.setCollectEmail(false);
  form.setProgressBar(true);
  form.setConfirmationMessage("Merci ! Enzo va lire ton art martial.");

  // 1. L'art martial
  form.addTextItem().setTitle('Ton pseudo').setRequired(true);
  form.addTextItem().setTitle("Nom de l'art martial")
      .setHelpText('Ex. : Iron Mountain Fist, Azure Cloud Sword Art').setRequired(true);
  form.addMultipleChoiceItem().setTitle('Son arme')
      .setChoiceValues(['Épée', 'Sabre', 'Lance', 'Poings', 'Éventail', 'Bâton', 'Dagues', 'Arc'])
      .showOtherOption(true).setRequired(true);
  form.addTextItem().setTitle("Son style en une phrase")
      .setHelpText('Ex. : rapide et fluide comme un nuage ; lourd et inarrêtable comme une montagne');

  // 2. Le passif
  form.addPageBreakItem().setTitle('Le passif')
      .setHelpText("Ce qui change tant que ce style est actif : les attaques de base, la parade, un bonus…");
  form.addTextItem().setTitle('Nom du passif');
  form.addParagraphTextItem().setTitle('Ce que fait le passif')
      .setHelpText('Ex. : chaque attaque de base réussie accélère la suivante ; une parade parfaite fait glisser l\'ennemi sur le côté.')
      .setRequired(true);

  // 3. Les 3 techniques (obligatoires)
  for (var i = 1; i <= 3; i++) {
    form.addPageBreakItem().setTitle('Technique ' + i)
        .setHelpText(i === 1 ? "Une technique se lance avec une touche, coûte du Qi et se recharge." : '');
    form.addTextItem().setTitle('Nom de la technique ' + i).setRequired(true);
    form.addParagraphTextItem().setTitle('Ce qu\'elle fait')
        .setHelpText('Ex. : une ruée de 5 blocs qui tranche tout sur son passage.').setRequired(true);
    form.addMultipleChoiceItem().setTitle('Elle est plutôt…')
        .setChoiceValues(['Rapide et légère', 'Moyenne', 'Lente et très forte']);
  }

  // 4. En plus, facultatif
  form.addPageBreakItem().setTitle('En plus (facultatif)');
  form.addParagraphTextItem().setTitle('Une 4e technique, un finish, ou d\'autres idées');

  Logger.log('Lien à envoyer à tes amis : ' + form.getPublishedUrl());
  Logger.log('Lien pour modifier et voir les réponses : ' + form.getEditUrl());
}
