package com.example.farooqi.importvcs.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SAMSUNG on 2/9/2018.
 */

public class User {
    String name;
    String fname;
    String tel;

    public User(String name, String fname, String tel) {
        this.name = name;
        this.fname = fname;
        this.tel = tel;
    }

    public String getName() {
          return name.replaceAll("[\\W+]", " ");
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }


    public static List<User> extracDataFromFile(List<String> results) {
        List<User> userData = new ArrayList<>();

        String name = "", fname = "", tel = "";


        for (int i = 0; i < results.size(); i++) {

            if (!(results.get(i).startsWith("END"))) {
                if (results.get(i).startsWith("N")) {
                    name = results.get(i).substring(2);
                } else if (results.get(i).startsWith("FN")) {
                    fname = results.get(i).substring(3);
                } else if (results.get(i).startsWith("TEL")) {
                    tel = results.get(i).substring(9);
                }
            } else {
                    userData.add(new User(name, fname, tel));
            }
        }
        return userData;
    }

}
