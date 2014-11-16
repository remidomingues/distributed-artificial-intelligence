/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package homework1.model;

import java.io.Serializable;
import java.util.GregorianCalendar;

/**
 * Museum artifact
 * @author Rémi Domingues <remidomingues@live.fr>
 */
public class Artifact implements Serializable {
    int id;
    String name;
    String author;
    GregorianCalendar createdAt;
    String country;
    ArtifactGenre genre;
    ArtifactCategory category;

    /**
     * Constructor
     * @param id
     * @param name
     * @param author
     * @param createdAt
     * @param country
     * @param genre
     * @param category 
     */
    public Artifact(int id, String name, String author, GregorianCalendar createdAt, String country, ArtifactGenre genre, ArtifactCategory category) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.createdAt = createdAt;
        this.country = country;
        this.genre = genre;
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

    public ArtifactGenre getGenre() {
        return genre;
    }    

    public ArtifactCategory getCategory() {
        return category;
    }    
}
