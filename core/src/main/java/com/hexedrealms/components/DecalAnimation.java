package com.hexedrealms.components;

public class DecalAnimation {
    private int step;
    private int currentAnimation;
    private boolean isFinal, isSingle;
    private float timer;
    public DecalAnimation(int step){
        this.step = step;
    }

    public void startAnimation(){
        isFinal = false;
    }
    public void UpdateDecal(float delta){
        if(isSingle && isFinal) return;

        timer += delta;
        currentAnimation = (int) timer;
        if(currentAnimation > step-1){
            currentAnimation = 0;
            timer = 0;
            isFinal = true;
        }
    }

    public void setFinalled(boolean finalled){
        isFinal = false;
    }


    public void setSingleUse(boolean isSingle){
        clearTimer();
        this.isFinal = false;
        this.isSingle = isSingle;
    }

    public void clearTimer(){
        currentAnimation = 0;
        timer = 0;
    }

    public void stopAnimation(){
        currentAnimation = 0;
        timer = 0;
    }

    public void setSize(int size){
        this.step = size;
    }

    public boolean isFinalled(){
        return isFinal;
    }
    public int getFrame(){
        return currentAnimation;
    }
}
