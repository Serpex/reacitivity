package com.example.jasper.reactivity;


import android.animation.Animator;
import android.animation.ObjectAnimator;

import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    //0 inactive / 1 active / 2 ended but active / -1 never played (uninitalized) / -2 starting phase
    static int gameStatus = -1;

    //0-50, ball size not included
    static int radius = 0;
    //diameter of ball, can only be changed with XML, wiull adapt automatically
    static int ballsize = -1;


    static int heightDisplay;
    static int widthDisplay;

    //Durations, will be set by stars
    static int durationBall = 10000;
    static int durationBasket = 5000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Hide Action Bar for Fullscreen
        ActionBar a = this.getSupportActionBar();
        a.hide();

        //find height and width of display
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        heightDisplay = displayMetrics.heightPixels;
        widthDisplay = displayMetrics.widthPixels;

        setContentView(R.layout.activity_main);


        //create Views of Basket and ball
        final ImageView basket = findViewById(R.id.imageView);
        basket.setVisibility(View.INVISIBLE);
        final ImageView ball = findViewById(R.id.fallPoint);
        ball.setVisibility(View.INVISIBLE);

        //create both animations
        final ObjectAnimator animationBall = ObjectAnimator.ofFloat(ball, "translationY", 800);
        animationBall.setDuration(durationBall);
        final ObjectAnimator animationBasket = ObjectAnimator.ofFloat(basket, "translationX", 1000);
        animationBasket.setDuration(durationBasket);


        //Ratingbar
        final RatingBar ratingBar = findViewById(R.id.ratingBar);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1);

        //WinningText
        final TextView winningText = findViewById(R.id.winningText);
        winningText.setVisibility(View.INVISIBLE);


        //defintions of buttons
        FloatingActionButton bMove = findViewById(R.id.floatingActionButton);
        final FloatingActionButton bFreeze = findViewById(R.id.floatingActionButton2);

        //Timeout handling
        final Handler timeOutHandler = new Handler();
        final Runnable runnableTimeout = new Runnable() {
            @Override
            public void run() {
                if (gameStatus == 1) {

                    say("Timeout");
                    postPlayProcedure(basket, ball, ratingBar, winningText, animationBasket, animationBall);
                }

            }

        };

        //the green play button
        bMove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //never played, therfore initalisation. Initalisation is earlier not possible because view.getWitdh would be 0
                if (gameStatus == -1) {

                    //nothing to do here till now
                    gameStatus = 0;
                }

                if (gameStatus == 2) return;

                if (gameStatus == 0) {
                    gameStatus = -2;

                    //Set Difficulty
                    setDifficulty(ratingBar);
                    animationBall.setDuration(durationBall);
                    animationBasket.setDuration(durationBasket);


                    //First iniialize ball, to have a diamater, then set ballsize, because makeViewBasket() needs Ballsize
                    makeViewBall(ball);
                    ballsize = ball.getWidth() / 2 + 1;
                    makeViewBasket(basket);

                    resetBallPosition(ball);

                    basket.setX((0 - basket.getWidth() / 2.0f));
                    resetBallPosition(ball);
                    fadeIn(basket);
                    fadeIn(ball);
                    fadeOut(ratingBar);
                    ratingBar.setActivated(false);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animationBall.start();
                            gameStatus = 1;
                        }
                    }, 2000);

                    //Timeout handler

                    timeOutHandler.postDelayed(runnableTimeout, durationBall);


                }


                changeBasketAnimation(basket, animationBasket);


                animationBasket.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {

                        if (gameStatus == 1) {

                            changeBasketAnimation(basket, animationBasket);

                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                });

                animationBasket.start();


            }
        });


        // the red freeze button

        bFreeze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (gameStatus == 1) {

                    //remove timeout runnable
                    timeOutHandler.removeCallbacks(runnableTimeout);

                    postPlayProcedure(basket, ball, ratingBar, winningText, animationBasket, animationBall);
                }
            }
        });




    }


    //get the curren position and chnages direction of speed according to it. CHanges speed itsself by distance to center
    public void changeBasketAnimation(ImageView basket, ObjectAnimator animationBasket) {

        float x = widthDisplay - (float) (basket.getWidth() / 2.0);
        if (basket.getX() > widthDisplay / 2.0 - basket.getWidth() / 2.0) {
            x = (float) (-1.0 * basket.getWidth() / 2.0);
        }

        animationBasket.setDuration((long) (durationBasket / (4 - (3 * Math.abs(basket.getX() + basket.getWidth() / 2 - widthDisplay / 2)) / (widthDisplay / 2))));
        animationBasket.setFloatValues(new float[]{x});
        animationBasket.start();

    }

    // proceudre after a game is ended. Either by Timeout or by user. 
    public void postPlayProcedure(ImageView basketT, ImageView ballT, RatingBar ratingBarT, TextView winningText, ObjectAnimator animationBasket, ObjectAnimator animationBall) {

        //if game already ended nothing should habppen
        if (gameStatus != 1) {
            return;
        }


        //gamestatus set to "ended but active"
        gameStatus = 2;

        //acced from withinner class therfore final declaration
        final ImageView basket = basketT;
        final ImageView ball = ballT;
        final RatingBar ratingBar = ratingBarT;


        //end all animations
        animationBasket.cancel();
        animationBall.cancel();


        //check if game was won
        if (isWin(basket, ball)) {

            // say("win");
            winProcedure(winningText);


        } else {
            say("Loose");

        }


        //reset of the game, fadout of basket and ball, fade in of ratingbar. Ganestatus will be set to 0(inactive9 AFTER that
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {


                fadeOut(basket);
                fadeOut(ball);
                fadeIn(ratingBar);
                ratingBar.setActivated(true);

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        gameStatus = 0;

                    }
                }, 1000);


            }
        }, 1000);


    }


    //setting all parameters according to the selected difficulty
    public void setDifficulty(RatingBar ratingBar) {


        say("Difficulty: " + ratingBar.getProgress() + " / 5");

        switch (ratingBar.getProgress()) {

            case 1:
                durationBall = 10000;
                durationBasket = 5000;
                radius = 50;
                break;

            case 2:
                durationBall = 9000;
                durationBasket = 4000;
                radius = 40;
                break;
            case 3:
                durationBall = 8000;
                durationBasket = 3000;
                radius = 25;
                break;
            case 4:
                durationBall = 7000;
                durationBasket = 2000;
                radius = 10;
                break;
            case 5:
                durationBall = 6000;
                durationBasket = 1000;
                radius = 0;
                break;

            default:
                durationBall = 10000;
                durationBasket = 5000;
                radius = 50;
                break;

        }


    }


    //fade in function for nor further specified view
    public void fadeIn(View view) {


        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0, 1f);
        fadeIn.setDuration(2000);
        fadeIn.start();
        view.setVisibility(View.VISIBLE);

    }

    //fade out function for nor further specified view
    public void fadeOut(View view) {

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0);
        fadeOut.setDuration(2000);
        fadeOut.start();

    }


    //reset the ball to the middle top
    public void resetBallPosition(View ball) {
        ball.setX(widthDisplay / 2 - ballsize / 2);
        ball.setY(0);
    }


    //Check if ball lies in Basket
    public boolean isWin(ImageView basket, ImageView ball) {


        float x = (basket.getX() + basket.getWidth() / 2.0f) - (ball.getX() + ball.getWidth() / 2.0f);
        float y = (basket.getY() + basket.getHeight() / 2.0f) - (ball.getY() + ball.getHeight() / 2.0f);

        double distance = Math.sqrt(x * x + y * y);

        double maxAllowedDistance = ballsize + radius + 1;

        return distance <= maxAllowedDistance;

    }


    //makes a view to a Basket
    public void makeViewBasket(View view) {

        //maximum Distance from edge of parent
        float maxD = view.getWidth() / 2.0f - ballsize;


        //Build the form itsself
        RoundRectShape roundRectShape = new RoundRectShape(new float[]{


                100, 100, 100, 100,
                100, 100, 100, 100}, new RectF(maxD - radius, maxD - radius, maxD - radius, maxD - radius), new float[]{
                maxD - radius, maxD - radius, maxD - radius, maxD - radius,
                maxD - radius, maxD - radius, maxD - radius, maxD - radius});


        ShapeDrawable shapeDrawable = new ShapeDrawable(roundRectShape);
        shapeDrawable.getPaint().setColor(Color.parseColor("#FF0000"));
        view.setBackground(shapeDrawable);


    }

    //makes a View to a ball
    public void makeViewBall(View view) {


        //Build the form itsself
        RoundRectShape roundRectShape = new RoundRectShape(new float[]{
                100, 100, 100, 100,
                100, 100, 100, 100}, null, null);


        ShapeDrawable shapeDrawable = new ShapeDrawable(roundRectShape);
        shapeDrawable.getPaint().setColor(Color.parseColor("#00FFFF"));
        view.setBackground(shapeDrawable);


    }

    //starts recursive blinking metod winProcedureBlinking()
    public static void winProcedure(View vT) {

        vT.setVisibility(View.VISIBLE);
        winProcedureBlinking(vT, 20);


        //YourView.setBackgroundColor(Color.argb(255, 255, 255, 255));
    }

    //just the blinking function for the method winprocedure
    public static void winProcedureBlinking(View vT, int nT) {

        final View v = vT;
        final int n = nT;

        if (n <= 0) {
            v.setVisibility(View.INVISIBLE);
            return;
        }
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (n % 2 == 0) {
                    v.setBackgroundColor(Color.argb(255, 255, 255, 0));
                } else {
                    v.setBackgroundColor(Color.argb(255, 255, 255, 255));
                }

                winProcedureBlinking(v, n - 1);

            }
        }, 100);


    }


    //toast generator
    public void say(String text) {

        Toast t = Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT);
        t.show();

    }
}
