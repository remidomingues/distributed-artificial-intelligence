/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package homework1.model;

import java.io.Serializable;

/**
 * Artifact description used for the selection (gender and category)
 * @author Rémi Domingues <remidomingues@live.fr>
 */
public class ArtifactDescription implements Serializable {
    ArtifactCategory category;
    ArtifactGenre genre;

    public ArtifactDescription(ArtifactCategory category, ArtifactGenre genre) {
        this.category = category;
        this.genre = genre;
    }

    public ArtifactCategory getCategory() {
        return category;
    }

    public ArtifactGenre getGenre() {
        return genre;
    }
}
