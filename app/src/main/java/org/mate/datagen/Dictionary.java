package org.mate.datagen;

import android.content.res.AssetManager;

import androidx.test.platform.app.InstrumentationRegistry;

import org.mate.MATE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

/**
 * Created by marceloeler on 27/07/17.
 */

public class Dictionary {
    //https://github.com/dwyl/english-words
    private static Vector<String> words = null;

    private static void loadWords(){
        AssetManager assetManager = InstrumentationRegistry.getInstrumentation().getTargetContext().getResources().getAssets();
        try {
            String[] files = assetManager.list("");
            for (String filestr: files){
                MATE.log(filestr);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        words = new Vector<String> ();
        InputStream file = null;
        try {
            file = InstrumentationRegistry.getInstrumentation().getTargetContext().getResources().getAssets().open("words.txt");
        } catch (IOException e) {
            e.printStackTrace();
            file=null;
        }

        if (file!=null){
            BufferedReader reader = new BufferedReader(new InputStreamReader(file));
            StringBuilder out = new StringBuilder();
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                   words.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

    }

    public static Vector<String> getWords(){
        if (words == null)
            loadWords();
        return words;
    }
}
