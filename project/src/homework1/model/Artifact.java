/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package homework1.model;

import java.util.GregorianCalendar;

/**
 *
 * @author Rémi Domingues <remidomingues@live.fr>
 */
public class Artifact {
    int id;
    String name;
    String author;
    GregorianCalendar createdAt;
    String country;
    ArtifactCategory category;

    public Artifact(int id, String name, String author, GregorianCalendar createdAt, String country, ArtifactCategory category) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.createdAt = createdAt;
        this.country = country;
        this.category = category;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public GregorianCalendar getCreatedAt() {
        return createdAt;
    }

    public String getCountry() {
        return country;
    }

    public ArtifactCategory getCategory() {
        return category;
    }
    
    
}
