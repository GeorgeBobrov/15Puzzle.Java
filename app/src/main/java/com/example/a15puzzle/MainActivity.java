package com.example.a15puzzle;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.Animator.AnimatorListener;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Random;


public class MainActivity extends AppCompatActivity {

	enum Mode {Game,
		GameOver,
		JustShuffled,
		PuzzleMatched };

	int base;
	Mode mode;

	Button[] tiles = new Button[0];
	long tileSize;
	long tileSpacing;
	long spaceX, spaceY;

	@ColorInt int tileFillNormalColor1 = 0xFFFFE4C4; //bisque
	@ColorInt int tileFillNormalColor2 = 0xFFABE024;

	long lastResizeTime;
	long lastTapTime;
	boolean closingAnimation = false;
	int timeRemaining;
	int panelDebugMaximumHeight;
	int resizeCount = 0;

	Handler handler = new Handler();
	Runnable timerTimeRunnable = new Runnable() {
		public void run()	{ timerTimeTimer(); }
	};
	Runnable timerResizeRunnable = new Runnable() {
		public void run()	{ timerResizeTimer(); }
	};
	Runnable timerCreateTilesRunnable = new Runnable() {
		public void run()	{ timerCreateTilesTimer(); }
	};

	private static Random randomGen = new Random();


	final float maxMoveAniDuration = 150;
	final float minMoveAniDuration = 1;

	TextView textTime;
	RelativeLayout panelClient;

	View.OnClickListener tileClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View sender) { OnTilePressed(sender); }
	};

	View.OnTouchListener tileTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View sender, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN){
				OnTilePressed(sender);
				return true;
			}
			return false;
		}
	};

	TimeInterpolator linear = new LinearInterpolator();
	TimeInterpolator inBack = new PathInterpolator(0.6f, -0.28f, 0.735f, 0.045f);
	TimeInterpolator outBack = new PathInterpolator(0.175f, 0.885f, 0.32f, 1.275f);
	TimeInterpolator outExpo = new PathInterpolator(0.19f, 1f, 0.22f, 1f);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		panelClient = findViewById(R.id.panelClient);
		textTime = findViewById(R.id.textTime);

		lastResizeTime = System.currentTimeMillis();   //To prevent resize on start on Android

		panelClient.getViewTreeObserver().addOnGlobalLayoutListener(
			new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				panelClientResize();
			}
		});

		setBase(4);
	}


	void  setMode(Mode value)
	{
		mode = value;
		if (mode == Mode.Game)
			handler.postDelayed(timerTimeRunnable, 1000);
		else
			handler.removeCallbacks(timerTimeRunnable);
	}

	public void buttonBaseOnClick(View sender)
	{
		Button senderButton = (Button) sender;
		char chrBase = senderButton.getText().charAt(0);
		String strBase = Character.toString(chrBase);
		int lBase = Integer.parseInt(strBase);
		setBase(lBase);
	}

	void setBase(int value)
	{
		if (value == base)
		{
			animateBaseNotChanged();
			return;
		}
		setMode(Mode.GameOver);
		animateTilesDisappeare();
		base = value;

		int delay;
		if ( tiles.length > 0)
			delay = (520 + 30 * tiles.length);
		else
			delay = (200);
		handler.postDelayed(timerCreateTilesRunnable, delay);
	}



	void timerCreateTilesTimer()
	{
		createTiles();
		setMaxTime();
		animatePrepareBeforePlace();
		animatePlaceTilesFast();
	}



	void createTiles()
	{
		for (int i = 0; i < tiles.length; i++)
			if (tiles[i] != null)
			{
//				GradientAnimation *gradientAni = (GradientAnimation*)tiles[i].property("gradientAni").value<void *>() ;
				((ViewGroup) tiles[i].getParent()).removeView(tiles[i]);
				tiles[i] = null;
			}

		tiles = new Button[base * base];
		for (int i = 0; i < tiles.length - 1; i++)
			if (tiles[i] == null)
			{
				final Button newTile;

				newTile = (Button) new Button(this);

//				newTile.setOnClickListener(tileClickListener);
				newTile.setOnTouchListener(tileTouchListener);

				newTile.setText(String.valueOf(i + 1));

				ValueAnimator colorAnimation = new ValueAnimator();
				colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

					@Override
					public void onAnimationUpdate(ValueAnimator animator) {
						newTile.setBackgroundTintList(ColorStateList.valueOf((int) animator.getAnimatedValue()));
					}

				});

				newTile.setTag(colorAnimation);


//				newTile.setStyleSheet(generateTileStyleSheet(tileFillNormalColor1, tileFillNormalColor2));
//				gradientAni.setCurColors(tileFillNormalColor1, tileFillNormalColor2);
				newTile.setBackgroundTintList(ColorStateList.valueOf(tileFillNormalColor1));

				newTile.setLayoutParams(new LayoutParams(100,100));

				panelClient.addView(newTile);

//			newTile.SendToBack;
				tiles[i] = newTile;
			}

		if (tiles[tiles.length - 1] != null)
			tiles[tiles.length - 1] = null;
	}


	int ind(int row, int col)
	{
		return row * base + col;
	}


	int actualPosition(Button tile)
	{
		for (int i = 0; i < tiles.length; i++)
			if (tiles[i] == tile)
				return i;
		return 0;
	}


	void OnTilePressed(View sender)
	{
		Button senderTile = (Button) sender;
		if (mode == Mode.JustShuffled)
			setMode(Mode.Game);
		boolean wasMoved = tryMoveTile(actualPosition(senderTile), maxMoveAniDuration, false);
		if (wasMoved)
			checkPuzzleMatched();
	}


	boolean tryMoveTile(int tilePosition, float moveAniDuration, boolean waitAnimationEnd)
	{
		boolean wasMoved = false;

		int colPressed = tilePosition % base;
		int rowPressed = tilePosition / base;

		for (int row = 0; row < base; row++)
			if (tiles[ind(row, colPressed)] == null)
			{
				int rowNoTile = row;
				if (rowNoTile > rowPressed) //Move tiles down
					for (int rowToMove = rowNoTile - 1; rowToMove >= rowPressed; rowToMove--)
					{
						moveTile(ind(rowToMove, colPressed), ind(rowToMove + 1, colPressed), moveAniDuration, waitAnimationEnd);
						wasMoved = true;
					}
				if (rowPressed > rowNoTile) //Move tiles up
					for (int rowToMove = rowNoTile + 1; rowToMove <= rowPressed; rowToMove++)
					{
						moveTile(ind(rowToMove, colPressed), ind(rowToMove - 1, colPressed), moveAniDuration, waitAnimationEnd);
						wasMoved = true;
					}
			}
		if (! wasMoved)
			for (int col = 0; col < base; col++)
				if (tiles[ind(rowPressed, col)] == null)
				{
					int colNoTile = col;
					if (colNoTile > colPressed) //Move tiles right
						for (int colToMove = colNoTile - 1; colToMove >= colPressed; colToMove--)
						{
							moveTile(ind(rowPressed, colToMove), ind(rowPressed, colToMove + 1), moveAniDuration, waitAnimationEnd);
							wasMoved = true;
						}
					if (colPressed > colNoTile) //Move tiles left
						for (int colToMove = colNoTile + 1; colToMove <= colPressed; colToMove++)
						{
							moveTile(ind(rowPressed, colToMove), ind(rowPressed, colToMove - 1), moveAniDuration, waitAnimationEnd);
							wasMoved = true;
						}
				}

		return wasMoved;
	}

	void moveTile(int oldPosition, int newPosition, float moveAniDuration, boolean waitAnimationEnd)
	{
		Button temp = tiles[newPosition];
		tiles[newPosition] = tiles[oldPosition];
		tiles[oldPosition] = temp;

		animateMoveTile(tiles[newPosition], moveAniDuration, waitAnimationEnd);
	};


	void animateMoveTile(Button tile, float moveAniDuration, boolean waitAnimationEnd)
	{
		int actPos = actualPosition(tile);
		int newCol = actPos % base;
		int newRow = actPos / base;

		float offsetOnScaledTile = (tileSize - tile.getLayoutParams().width) / 2.0f;

		long x = spaceX + Math.round(newCol * (tileSize + tileSpacing) + offsetOnScaledTile);
		long y = spaceY + Math.round(newRow * (tileSize + tileSpacing) + offsetOnScaledTile);

		if (moveAniDuration > 0)
		{
//			animatePropertyDelay(tile, "geometry", geometry, moveAniDuration, 0, QEasingCurve.OutExpo, true, waitAnimationEnd);

			tile.animate().translationX(x).translationY(y)
					.setDuration((long) moveAniDuration).setStartDelay(0)
					.setInterpolator(outExpo);

		}
		else
		{
			tile.setTranslationX(x);
			tile.setTranslationY(y);
		}
	}


	void checkPuzzleMatched()
	{
		boolean puzzleMatched = true;
		for (int i = 0; i < tiles.length; i++)
			if (tiles[i] != null)
			{
				int textNumber = Integer.parseInt( tiles[i].getText().toString() );

				if ((textNumber - 1) != actualPosition(tiles[i]))
				{
					puzzleMatched = false;
					break;
				}
			}

		if (puzzleMatched && (mode == Mode.Game))
		{
			setMode(Mode.PuzzleMatched);
			animatePuzzleMatched();
		}
		if ((! puzzleMatched) && ((mode == Mode.PuzzleMatched) || (mode == Mode.JustShuffled)))
		{
			animateNormalizeTilesColor();
			if (mode == Mode.PuzzleMatched)
				setMode(Mode.GameOver);
		}
	}



	public void buttonShuffleOnClick(View sender)
	{
		animateNormalizeTilesColor();

		int newI = 0;
		int moveCount = tiles.length * tiles.length;
		float moveAniDuration = maxMoveAniDuration;
		for (int i = 1; i <= moveCount; i++)
		{
			if (i <= 10)
				moveAniDuration = minMoveAniDuration + (maxMoveAniDuration * (1 - ((i) / 10.0f)));
			if (i >= moveCount - 10)
				moveAniDuration = minMoveAniDuration + ((maxMoveAniDuration / 2) * (1 - ((moveCount - i) / 10.0f)));
			if ((i > 20) && (i < moveCount - 20))
				if ((i % 10) == 0)
					moveAniDuration = minMoveAniDuration;
				else
					moveAniDuration = 0;

			boolean wasMoved;
			do
			{
				newI = randomGen.nextInt(tiles.length);
				wasMoved =  tryMoveTile(newI, /*moveAniDuration*/0, true);
			}
			while (! wasMoved);
		}
		setMaxTime();
//  stopBlinkShuffle();

		setMode(Mode.JustShuffled);
		checkPuzzleMatched();
	}


	void timerTimeTimer()
	{
		Log.d("Timer", "timerTimeTimer");

		timeRemaining = timeRemaining - 1;

		int sec = timeRemaining % 60;
		int min = timeRemaining / 60;

		textTime.setText(String.format("%1$d:%2$02d", min, sec));

		if (timeRemaining == 0)
		{
			setMode(Mode.GameOver);
			animateTimeOver();
//		startBlinkShuffle();
			return;
		}
		if (timeRemaining <= 10)
			animateTimeRunningOut();

		if (mode == Mode.Game) {

			handler.postDelayed(timerTimeRunnable, 1000);
			Log.d("Timer", "timerTime.postDelayed(timerTimeRunnable, 1000) in timerTimeTimer");
		}


	}


	void setMaxTime()
	{
		timeRemaining = ((base * base * base * base) / 20) * 10;
		int sec = timeRemaining % 60;
		int min = timeRemaining / 60;
		textTime.setText(String.format("%1$d:%2$02d", min, sec));
	}


	void panelClientResize()
	{
		handler.removeCallbacks(timerResizeRunnable);
		handler.postDelayed(timerResizeRunnable, 200);
	}


	void timerResizeTimer()
	{
		handler.removeCallbacks(timerResizeRunnable);

		long timeFromLastResize_ms = System.currentTimeMillis() - lastResizeTime;

		if (timeFromLastResize_ms > 1000)
		{
			animatePlaceTilesFast();
			lastResizeTime = System.currentTimeMillis();
		}
	}

	@Override
	public void onBackPressed() {
		if (! closingAnimation)
		{
			closingAnimation = true;
			animateTilesDisappeare();
			return;
		}
		finish();
	}

//-------------------------------   Animations   -----------------------------
	void calcConsts()
	{
		int height = (panelClient.getMeasuredHeight());
		int width = (panelClient.getMeasuredWidth());

		if (height > width)
		{
			spaceX = Math.round(((float)width) / 20);
			tileSize = Math.round(((float)(width - spaceX * 2)) / base);
			spaceY = spaceX + Math.round(((float)(height - width)) / 2);
		}
		else
		{
			spaceY = Math.round(((float)height) / 20);
			tileSize = Math.round(((float)(height - spaceY * 2)) / base);
			spaceX = spaceY + Math.round(((float)(width - height)) / 2);
		}
		tileSpacing = Math.round(tileSize * 0.06);
		tileSize = Math.round(tileSize * 0.94);
		spaceX = spaceX + Math.round(((float)tileSpacing) / 2);
		spaceY = spaceY + Math.round(((float)tileSpacing) / 2);

	}

	void animatePlaceTilesFast()
	{
		calcConsts();
		Log.d("Animate", "PlaceTilesFast");
		for (int i = 0; i < tiles.length; i++)
			if (tiles[i] != null)
			{
				Button tile = tiles[i];
				int delay = 30 * i; //delay for tile

				int col = i % base;
				int row = i / base;

				int width = tile.getLayoutParams().width;
				int height = tile.getLayoutParams().height;

				Log.d("Animate", String.format("width=%d, height=%d", width, height));

				float scaleX = ((float)tileSize) / width;
				float scaleY = ((float)tileSize) / height;

				float offsetOnScaledTile = (tileSize - width) / 2.0f;

				long x = spaceX + Math.round(col * (width * scaleX + tileSpacing) + offsetOnScaledTile);
				long y = spaceY + Math.round(row * (height *scaleY + tileSpacing) + offsetOnScaledTile);

//				tile.animate().scaleX(scaleX).scaleY(scaleY)
//						.translationX(x).translationY(y)
//						.setDuration(100).setStartDelay(100l + delay).setInterpolator(linear);
				Log.d("Animate", String.format("x=%d, y=%d, scaleX=%g, scaleY=%g, ", x, y, scaleX, scaleY));

				animateFloatDelay(tile, "scaleX", scaleX, 200, 200 + delay);
				animateFloatDelay(tile, "scaleY", scaleY, 200, 100 + delay);
				animateFloatDelay(tile, "translationX", x, 200, delay);
				animateFloatDelay(tile, "translationY", y, 100, delay);
			}

	}

	void animateTilesDisappeare()
	{
		Button lastTile = null;
		for (int i = 0; i < tiles.length; i++)
			if (tiles[i] != null)
			{
				Button tile = tiles[i];
				int delay = 30 * i; //delay for tile

				long x = Math.round(tile.getTranslationX() + ((tileSize) / 2.0));
				long y = Math.round(tile.getTranslationY() + tileSize) ;

				tile.animate().scaleX(0.1f).scaleY(0.1f)
						.rotation(45.0f).alpha(0)
						.translationX(x).translationY(y)
						.setDuration(400).setStartDelay(delay).setInterpolator(inBack);

//				animateFloatDelay(tile, "scaleX", 0.1f, 400, delay);
//				animateFloatDelay(tile, "scaleY", 0.1f, 400, delay);
//				animateFloatDelay(tile, "rotation", 45, 400, delay);
//				animateFloatDelay(tile, "translationY", y, 400, delay, inBack);
//				animateFloatDelay(tile, "translationX", x, 400, delay);
//				animateFloatDelay(tile, "alpha", 0, 400, 100 + delay);

				lastTile = tile;
			}

//		Log.d("closingAnimation", " = " + ((Boolean)closingAnimation).toString() +
//				" lastTile =" + ((lastTile == null)? "null": lastTile.toString())
//		);

		if (closingAnimation && (lastTile != null)) {

			lastTile.animate().setListener(new AnimatorListener() {
				@Override
				public void onAnimationEnd(Animator animation) {
					finish();
				}

				public void onAnimationStart(Animator animation) {}
				public void onAnimationCancel(Animator animation) {}
				public void onAnimationRepeat(Animator animation) {}
			});
		}

	}

	void animatePrepareBeforePlace()
	{
		for (int i = 0; i < tiles.length; i++)
			if (tiles[i] != null) {
				Button tile = tiles[i];

				float scaleX = ((float)tileSize) / tile.getLayoutParams().width;
				float scaleY = ((float)tileSize) / tile.getLayoutParams().height;

				int col = i % base;
				int row = i / base;

				long x = spaceX + Math.round(col * (tile.getLayoutParams().width * scaleX + tileSpacing));
				long y = spaceY + Math.round(row * (tile.getLayoutParams().height * scaleY + tileSpacing));

				tile.setScaleX(0.5f);
				tile.setScaleY(0.5f);

				tile.setAlpha(0);
				tile.setRotation(45.0f);
				tile.setTranslationX(x + Math.round((tileSize) / 2.0));
				tile.setTranslationY(y + tileSize);

			}

		for (int i = 0; i < tiles.length; i++)
			if (tiles[i] != null) {
				Button tile = tiles[i];
				int delay = 30 * i; //delay for tile

//				tile.animate().rotation(0).alpha(1)
//						.setDuration(200).setStartDelay(delay).setInterpolator(linear);

				animateFloatDelay(tile, "rotation", 0, 400, delay);
				animateFloatDelay(tile, "alpha", 1, 400, 100 + delay);
			}

	}

	void animateBaseNotChanged()
	{
		for (int i = 0; i < tiles.length; i++)
			if (tiles[i] != null) {
				Button tile = tiles[i];
				int delay = 30 * i; //delay for tile

				float origScaleX = tile.getScaleX();
				float origScaleY = tile.getScaleY();

				animateFloatDelay(tile, "scaleX", origScaleX / 2.0f, 300, delay, inBack);
				animateFloatDelay(tile, "scaleY", origScaleY / 2.0f, 300, delay, inBack);

				animateFloatDelay(tile, "scaleX", origScaleX, 300, 350 + delay, outBack);
				animateFloatDelay(tile, "scaleY", origScaleY, 300, 350 + delay, outBack);
			}
	}

	void animatePuzzleMatched(){
		for (int i = 0; i < tiles.length; i++)
			if (tiles[i] != null) {
				final Button tile = tiles[i];
				int delay = 30 * i; //delay for tile

				animateFloatDelay(tile, "rotation", 360, 1000, 350, outBack);

//				tile.setBackgroundTintList(colorStateList.valueOf(/*lawngreen*/0xFF7CFC00));

				ValueAnimator colorAnimation = (ValueAnimator) tile.getTag();
				int colorFrom = tile.getBackgroundTintList().getDefaultColor();
				int colorTo = /*lawngreen*/0xFF7CFC00;
				colorAnimation.setObjectValues(colorFrom, colorTo);
				colorAnimation.setEvaluator(new ArgbEvaluator());
				colorAnimation.setDuration(1000);
				colorAnimation.setStartDelay(delay);
				colorAnimation.setRepeatCount(0);
				colorAnimation.start();
			}

	}

	void animateTimeRunningOut()
	{
		for (int i = 0; i < tiles.length; i++)
			if (tiles[i] != null) {
				final Button tile = tiles[i];

//				tile.setBackgroundTintList(colorStateList.valueOf(/*darkorange*/0xFFFF8C00));

				ValueAnimator colorAnimation = (ValueAnimator) tile.getTag();
				int colorFrom = tile.getBackgroundTintList().getDefaultColor();
				int colorTo = /*darkorange*/0xFFFF8C00;
				colorAnimation.setObjectValues(colorFrom, colorTo);
				colorAnimation.setEvaluator(new ArgbEvaluator());
				colorAnimation.setDuration(150);
				colorAnimation.setStartDelay(0);
				colorAnimation.setRepeatCount(1);
				colorAnimation.setRepeatMode(ObjectAnimator.REVERSE);
				colorAnimation.start();
			}

	}

	void animateTimeOver(){
		for (int i = 0; i < tiles.length; i++)
			if (tiles[i] != null) {
				final Button tile = tiles[i];
				int delay = 30 * i; //delay for tile
//				tile.setBackgroundTintList(colorStateList.valueOf(/*red*/0xFFFF0000));

				ValueAnimator colorAnimation = (ValueAnimator) tile.getTag();

				int colorFrom = tile.getBackgroundTintList().getDefaultColor();
				int colorTo = /*red*/0xFFFF0000;
				colorAnimation.setObjectValues(colorFrom, colorTo);
				colorAnimation.setEvaluator(new ArgbEvaluator());
				colorAnimation.setDuration(1000);
				colorAnimation.setStartDelay(delay);
				colorAnimation.setRepeatCount(0);
				colorAnimation.start();
			}

	}

	void animateNormalizeTilesColor(){
		for (int i = 0; i < tiles.length; i++)
			if (tiles[i] != null) {
				final Button tile = tiles[i];
				int delay = 30 * i; //delay for tile
//				tile.setBackgroundTintList(colorStateList.valueOf(tileFillNormalColor1));

				ValueAnimator colorAnimation = (ValueAnimator) tile.getTag();
				int colorFrom = tile.getBackgroundTintList().getDefaultColor();
				int colorTo = tileFillNormalColor1;
				colorAnimation.setObjectValues(colorFrom, colorTo);
				colorAnimation.setEvaluator(new ArgbEvaluator());
				colorAnimation.setDuration(1000);
				colorAnimation.setStartDelay(delay);
				colorAnimation.setRepeatCount(0);
				colorAnimation.start();
			}

	}

	void showDebug(){}

//-------------------------------  Test different Animations   -----------------------------

	public void buttonDisappeareOnClick(View sender)
	{
		animateTilesDisappeare();
	}

	public void buttonPlaceOnClick(View sender)
	{
		animateNormalizeTilesColor();
		animatePrepareBeforePlace();
		animatePlaceTilesFast();
	}

	public void buttonTimeOverOnClick(View sender)
	{
		animateTimeOver();
	}

	public void buttonTimeRunningOutOnClick(View sender)
	{
		animateTimeRunningOut();
	}

	public void buttonPuzzleMatchedOnClick(View sender)
	{
		animatePuzzleMatched();
	}

//---------------------------  Realization of Property Animation   -----------------------------

	ObjectAnimator animateFloatDelayWait(View target, String propertyName,
	                                 float value, long duration_ms, long delay_ms)
	{
		return animateFloatDelay(target, propertyName, value, duration_ms, delay_ms, linear);
	}

	ObjectAnimator animateFloatDelay(View target, String propertyName,
	   float value, long duration_ms, long delay_ms)
	{
		return animateFloatDelay(target, propertyName, value, duration_ms, delay_ms, linear);
	}

	ObjectAnimator animateFloatDelay(View target, String propertyName,
      float value, long duration_ms, long delay_ms,
      TimeInterpolator interpolator/*, boolean deleteWhenStopped, boolean waitAnimationEnd*/)
	{
		ObjectAnimator objectAnimator	= ObjectAnimator.ofFloat(target, propertyName, value);
		objectAnimator.setDuration(duration_ms);
		objectAnimator.setStartDelay(delay_ms);
//		objectAnimator.setRepeatCount(1);
//		objectAnimator.setRepeatMode(ObjectAnimator.REVERSE);
		objectAnimator.setInterpolator(interpolator);
		objectAnimator.start();
		return objectAnimator;
	}

}
