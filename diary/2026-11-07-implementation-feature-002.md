# 2026-11-07 — Implémentation de la première feature : les flux tombent juste, les permissions non

Passage à l'implémentation de la première vraie feature, le moteur du jeu
(`specs/002-cornucopia-game-flow/`). Globalement, ça se passe bien — et cette
fois, les flux de spec-kit sont bien plus logiques que lors du bootstrap :
quand la spec raconte une histoire (le maître du jeu qui orchestre les tours,
les archétypes Grumpy/Happy/Greedy/Prof), les phases du `tasks.md` suivent
naturellement, avec le cœur du MVP (la boucle de tour) en premier.

## Le vrai point de frustration : les permissions

Il y a quand même un point de friction pénible : le nombre de demandes de
permission pour les commandes, les URL, etc. C'est lourd. C'est cohérent avec
ce que je vis au boulot — GitHub Copilot pose le même genre de questions, mais
sans le flux spec-kit derrière — mais là, pour un projet jouet, ça casse les
pieds. On passe son temps à valider des `./mvnw test`.

## JetBrains Air : plusieurs agents en parallèle, et surtout la revue de code

Côté JetBrains Air, il y a un avantage concret : je peux faire tourner
plusieurs agents en parallèle, dans l'interface complète de l'IDE. Et il y a
surtout — c'est la première fois que je peux réellement en profiter — un moyen
de faire de la **revue du code des agents de manière poussée et bien intégrée
à l'IDE**. Le mécanisme tient en un commentaire posé sur le code généré, puis
« finish & attach comment » : l'agent reprend la main avec le commentaire
attaché.

## Deux choses remarquées dans le code de Mistral

La revue dans l'IDE m'a permis de remarquer deux tics du code généré par
Mistral.

**Premier tic : le pattern « une interface, une multitude de records qui
l'implémentent ».** Ça peut être utile, mais parfois ce n'est pas justifié.
Exemple avec la réponse du joueur à une question :

```java
public sealed interface Answer {
    record Knows(String answer) implements Answer {}
    record DoesNotKnow() implements Answer {}
}
```

Ici, la *sealed interface* se défend (deux cas, un payload sur l'un d'eux), mais
Mistral la ressort un peu systématiquement. Un petit commentaire dans la revue,
« finish & attach comment », et c'est réglé.

**Deuxième tic, qui m'a arraché un sourire : Mistral code en français quand on
lui donne des specs en français.** La spec 002 est en français, et le code
suit — avec des classes et méthodes comme `AtelierStore`, `Atelier`,
`saveAtelier`, `newAtelierId` :

```java
public class AtelierStore {
    // ...
    public String newAtelierId() {
        return "atelier" + LocalDateTime.now().format(ID_FORMAT);
    }

    public void saveAtelier(String atelierId, Atelier atelier) {
        writeJson(rootDir.resolve(atelierId).resolve("atelier.json"), atelier);
    }
}
```

Techniquement ça compile et ça marche, mais du Java en français, c'est
inédit — et ça valait bien un sourire. La suite au prochain épisode.
