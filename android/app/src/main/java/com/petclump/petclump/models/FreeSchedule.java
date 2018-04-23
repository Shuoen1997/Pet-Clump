package com.petclump.petclump.models;

class FreeSchedule{
    boolean[][] freeMatrix;

    String freeString;

    enum PartDay{ Morning, AfterNoon, Night};
    enum WeekDay{
        Monday,Tuesday,Wednesday,Thursday,Friday,Saturday,Sunday
    }


    public boolean isFree(WeekDay weekDay, PartDay partDay){
        return freeMatrix[weekDay.ordinal()][partDay.ordinal()];
    }

    public FreeSchedule(String freeString){
        this.freeString = freeString;
        // initialize freeMatrix
        for(int i=0; i<7; i++){
            freeMatrix[i] = new boolean[3];
            for(int j=0; j<3; j++){
                freeMatrix[i][j] = false;
            }
        }
        String chars = new String(freeString);
        int manCounter = 0, weekCounter = 0;
        if (chars.length() != 21) {
            chars = "000000000000000000000";
        }
        for(int i=0; i<freeString.length(); i++){
            // 0 is not free, 1 is free
            freeMatrix[weekCounter][manCounter] = chars.charAt(i) == '1';
            manCounter += 1;
            manCounter %= 3;
            if (manCounter == 0) { weekCounter += 1; }
        }
    }
}