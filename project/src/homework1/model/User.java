/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package homework1.model;

import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class User {
    int age;
    Occupation occupation;
    Gender gender;
    List<ArtifactCategory> interests;
    List<Artifact> visitedArtifacts = new LinkedList<>();
    
    public User(Gender gender, Occupation occupation, int age, List<ArtifactCategory> interests) {
        this.age = age;
        this.occupation = occupation;
        this.interests = interests;
    }

    public int getAge() {
        return age;
    }

    public Occupation getOccupation() {
        return occupation;
    }

    public Gender getGender() {
        return gender;
    }

    public List<ArtifactCategory> getInterests() {
        return interests;
    }

    public List<Artifact> getVisitedArtifacts() {
        return visitedArtifacts;
    }

}
