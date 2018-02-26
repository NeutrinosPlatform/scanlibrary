package com.scanlibrary;

/**
 * Created by sekar on 21/2/18.
 */

class Loader {
    private static boolean done = false;

    protected static synchronized void load() {
        if (done)
            return;

        System.loadLibrary("opencv_java3");
        System.loadLibrary("Scanner");

        done = true;
    }
}

