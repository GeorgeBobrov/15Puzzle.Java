package com.example.a15puzzle;

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
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Random;


public class MainActivity extends AppCompatActivity {

	enum TMode {Game,
		GameOver,
		JustShuffled,
		PuzzleMatched };

	int Base;
	TMode Mode;

	Button[] Tiles;
	long TileSize;
	long TileSpacing;
	long SpaceX, SpaceY;

	int TileFillNormalColor1, TileFillNormalColor2;
	long LastResizeTime;
	long LastTapTime;
	boolean ClosingAnimation = false;
	boolean GreenTiles;
	int TimeRemaining;
	int PanelDebugMaximumHeight;
	int ResizeCount = 0;

	Handler TimerTime;
	Handler TimerResize;
	Handler TimerCreateTiles;
	Runnable TimerTimeRunnable;
	Runnable TimerResizeRunnable;
	Runnable TimerCreateTilesRunnable;

	private static Random RandomGen;


	final float MaxMoveAniDuration = 150;
	final float MinMoveAniDuration = 1;

	TextView TextTime;
	RelativeLayout PanelClient;
	View.OnClickListener TileClickListener;
	View.OnTouchListener TileTouchListener;

	TimeInterpolator linear;
	TimeInterpolator inBack;
	TimeInterpolator outBack;
	TimeInterpolator outExpo;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		PanelClient = findViewById(R.id.PanelClient);
//		mStartButton = findViewById(R.id.buttonStart);
		TextTime = findViewById(R.id.TextTime);

		Tiles = new Button[0];


		TimerTime = new android.os.Handler();
		TimerResize = new android.os.Handler();
		TimerCreateTiles = new android.os.Handler();

		TimerTimeRunnable = new Runnable() {
			public void run()	{ TimerTimeTimer(); }
		};

		TimerResizeRunnable = new Runnable() {
			public void run()	{ TimerResizeTimer(); }
		};

		TimerCreateTilesRunnable = new Runnable() {
			public void run()	{ TimerCreateTilesTimer(); }
		};

		LastResizeTime = System.currentTimeMillis();   //To prevent resize on start on Android

		TileClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View sender) { OnTilePressed(sender); }
		};

		TileTouchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View sender, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN){
					OnTilePressed(sender);
					return true;
				}
				return false;
			}

		};


		TileFillNormalColor1 = 0xFFFFE4C4; //bisque
		TileFillNormalColor2 = 0xFFABE024;

		linear = new LinearInterpolator();
		inBack = new PathInterpolator(0.6f, -0.28f, 0.735f, 0.045f);
		outBack = new PathInterpolator(0.175f, 0.885f, 0.32f, 1.275f);
		outExpo = new PathInterpolator(0.19f, 1f, 0.22f, 1f);

//  PanelClient  background-color: rgb(244, 244, 244);

		RandomGen = new Random();
		SetBase(4);

	}

	void  SetMode(TMode Value)
	{
		Mode = Value;
		if (Mode == TMode.Game)
			TimerTime.postDelayed(TimerTimeRunnable, 1000);
		else
			TimerTime.removeCallbacks(TimerTimeRunnable);
	}

	public void ButtonBaseOnClick(View sender)
	{
		Button SenderButton = (Button) sender;
		char chrBase = SenderButton.getText().charAt(0);
		String strBase = Character.toString(chrBase);
		int LBase = Integer.parseInt(strBase);
		SetBase(LBase);
	}

	void SetBase(int Value)
	{
		if (Value == Base)
		{
			AnimateBaseNotChanged();
			return;
		}
		SetMode(TMode.GameOver);
		AnimateTilesDisappeare();
		Base = Value;
		SetMaxTime();

		int dalay;
		if ( Tiles.length > 0)
			dalay = (520 + 30 * Tiles.length);
		else
			dalay = (200);
		TimerCreateTiles.postDelayed(TimerCreateTilesRunnable, dalay);
	}



	void TimerCreateTilesTimer()
	{
		CreateTiles();
		AnimatePrepareBeforePlace();
		AnimatePlaceTilesFast();
	}



	void CreateTiles()
	{
		for (int i = 0; i < Tiles.length; i++)
			if (Tiles[i] != null)
			{
//				TGradientAnimation *GradientAni = (TGradientAnimation*)Tiles[i].property("GradientAni").value<void *>() ;
				((ViewGroup) Tiles[i].getParent()).removeView(Tiles[i]);
				Tiles[i] = null;
			}

		Tiles = new Button[Base * Base];
		for (int i = 0; i < Tiles.length - 1; i++)
			if (Tiles[i] == null)
			{
				final Button NewTile;

				NewTile = (Button) new Button(this);

//				NewTile.setOnClickListener(TileClickListener);
				NewTile.setOnTouchListener(TileTouchListener);

				NewTile.setText(String.valueOf(i + 1));

				ValueAnimator colorAnimation = new ValueAnimator();
				colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

					@Override
					public void onAnimationUpdate(ValueAnimator animator) {
						NewTile.setBackgroundTintList(ColorStateList.valueOf((int) animator.getAnimatedValue()));
					}

				});

				NewTile.setTag(colorAnimation);


//				NewTile.setStyleSheet(GenerateTileStyleSheet(TileFillNormalColor1, TileFillNormalColor2));
//				GradientAni.SetCurColors(TileFillNormalColor1, TileFillNormalColor2);
				NewTile.setBackgroundTintList(ColorStateList.valueOf(TileFillNormalColor1));

				NewTile.setLayoutParams(new LayoutParams(100,100));

				PanelClient.addView(NewTile);

//			NewTile.SendToBack;
				Tiles[i] = NewTile;
			}

		if (Tiles[Tiles.length - 1] != null)
			Tiles[Tiles.length - 1] = null;
	}


	int ind(int Row, int Col)
	{
		return Row * Base + Col;
	}


	int ActualPosition(Button ATile)
	{
		for (int i = 0; i < Tiles.length; i++)
			if (Tiles[i] == ATile)
				return i;
		return 0;
	}


	void OnTilePressed(View sender)
	{
		Button SenderTile = (Button) sender;
		if (Mode == TMode.JustShuffled)
			SetMode(TMode.Game);
		boolean WasMoved = TryMoveTile(ActualPosition(SenderTile), MaxMoveAniDuration, false);
		if (WasMoved)
			CheckPuzzleMatched();
	}


	boolean TryMoveTile(int TilePosition, float MoveAniDuration, boolean WaitAnimationEnd)
	{
		boolean WasMoved = false;

		int ColPressed = TilePosition % Base;
		int RowPressed = TilePosition / Base;

		for (int Row = 0; Row < Base; Row++)
			if (Tiles[ind(Row, ColPressed)] == null)
			{
				int RowNoTile = Row;
				if (RowNoTile > RowPressed) //Move tiles down
					for (int RowToMove = RowNoTile - 1; RowToMove >= RowPressed; RowToMove--)
					{
						MoveTile(ind(RowToMove, ColPressed), ind(RowToMove + 1, ColPressed), MoveAniDuration, WaitAnimationEnd);
						WasMoved = true;
					}
				if (RowPressed > RowNoTile) //Move tiles up
					for (int RowToMove = RowNoTile + 1; RowToMove <= RowPressed; RowToMove++)
					{
						MoveTile(ind(RowToMove, ColPressed), ind(RowToMove - 1, ColPressed), MoveAniDuration, WaitAnimationEnd);
						WasMoved = true;
					}
			}
		if (! WasMoved)
			for (int Col = 0; Col < Base; Col++)
				if (Tiles[ind(RowPressed, Col)] == null)
				{
					int ColNoTile = Col;
					if (ColNoTile > ColPressed) //Move tiles right
						for (int ColToMove = ColNoTile - 1; ColToMove >= ColPressed; ColToMove--)
						{
							MoveTile(ind(RowPressed, ColToMove), ind(RowPressed, ColToMove + 1), MoveAniDuration, WaitAnimationEnd);
							WasMoved = true;
						}
					if (ColPressed > ColNoTile) //Move tiles left
						for (int ColToMove = ColNoTile + 1; ColToMove <= ColPressed; ColToMove++)
						{
							MoveTile(ind(RowPressed, ColToMove), ind(RowPressed, ColToMove - 1), MoveAniDuration, WaitAnimationEnd);
							WasMoved = true;
						}
				}

		return WasMoved;
	}

	void MoveTile(int OldPosition, int NewPosition, float MoveAniDuration, boolean WaitAnimationEnd)
	{
		Button temp = Tiles[NewPosition];
		Tiles[NewPosition] = Tiles[OldPosition];
		Tiles[OldPosition] = temp;

		AnimateMoveTile(Tiles[NewPosition], MoveAniDuration, WaitAnimationEnd);
	};


	void AnimateMoveTile(Button ATile, float MoveAniDuration, boolean WaitAnimationEnd)
	{
		int ActPos = ActualPosition(ATile);
		int NewCol = ActPos % Base;
		int NewRow = ActPos / Base;

		float OffsetOnScaledTile = (TileSize - ATile.getLayoutParams().width) / 2.0f;

		long X = SpaceX + Math.round(NewCol * (TileSize + TileSpacing) + OffsetOnScaledTile);
		long Y = SpaceY + Math.round(NewRow * (TileSize + TileSpacing) + OffsetOnScaledTile);

		if (MoveAniDuration > 0)
		{
//			AnimatePropertyDelay(ATile, "geometry", geometry, MoveAniDuration, 0, QEasingCurve.OutExpo, true, WaitAnimationEnd);

			ATile.animate().translationX(X).translationY(Y)
					.setDuration((long) MoveAniDuration).setStartDelay(0)
					.setInterpolator(outExpo);

		}
		else
		{
			ATile.setTranslationX(X);
			ATile.setTranslationY(Y);
		}
	}


	void CheckPuzzleMatched()
	{
		boolean LPuzzleMatched = true;
		for (int i = 0; i < Tiles.length; i++)
			if (Tiles[i] != null)
			{
				int TextNumber = Integer.parseInt( Tiles[i].getText().toString());

				if ((TextNumber - 1) != ActualPosition(Tiles[i]))
				{
					LPuzzleMatched = false;
					break;
				}
			}

		if (LPuzzleMatched && (Mode == TMode.Game))
		{
			SetMode(TMode.PuzzleMatched);
			AnimatePuzzleMatched();
		}
		if ((! LPuzzleMatched) && ((Mode == TMode.PuzzleMatched) || (Mode == TMode.JustShuffled)))
		{
			AnimateNormalizeTilesColor();
			if (Mode == TMode.PuzzleMatched)
				SetMode(TMode.GameOver);
		}
	}



	public void ButtonShuffleOnClick(View sender)
	{
		AnimateNormalizeTilesColor();

		int NewI = 0;
		int MoveCount = Tiles.length * Tiles.length;
		float MoveAniDuration = MaxMoveAniDuration;
		for (int i = 1; i <= MoveCount; i++)
		{
			if (i <= 10)
				MoveAniDuration = MinMoveAniDuration + (MaxMoveAniDuration * (1 - ((i) / 10.0f)));
			if (i >= MoveCount - 10)
				MoveAniDuration = MinMoveAniDuration + ((MaxMoveAniDuration / 2) * (1 - ((MoveCount - i) / 10.0f)));
			if ((i > 20) && (i < MoveCount - 20))
				if ((i % 10) == 0)
					MoveAniDuration = MinMoveAniDuration;
				else
					MoveAniDuration = 0;

			boolean WasMoved;
			do
			{
				NewI = RandomGen.nextInt(Tiles.length);
				WasMoved =  TryMoveTile(NewI, /*MoveAniDuration*/0, true);
			}
			while (! WasMoved);
		}
		SetMaxTime();
//  StopBlinkShuffle();

		SetMode(TMode.JustShuffled);
		CheckPuzzleMatched();
	}


	void TimerTimeTimer()
	{
		Log.d("Timer", "TimerTimeTimer");

		TimeRemaining = TimeRemaining - 1;

		int Sec = TimeRemaining % 60;
		int Min = TimeRemaining / 60;

		TextTime.setText(String.format("%1$d:%2$02d", Min, Sec));

		if (TimeRemaining == 0)
		{
			SetMode(TMode.GameOver);
			AnimateTimeOver();
//		StartBlinkShuffle();
			return;
		}
		if (TimeRemaining <= 10)
			AnimateTimeRunningOut();

		if (Mode == TMode.Game) {

			TimerTime.postDelayed(TimerTimeRunnable, 1000);
			Log.d("Timer", "TimerTime.postDelayed(TimerTimeRunnable, 1000) in TimerTimeTimer");
		}


		}


	void SetMaxTime()
	{
		TimeRemaining = ((Base * Base * Base * Base) / 20) * 10;
		int Sec = TimeRemaining % 60;
		int Min = TimeRemaining / 60;
		TextTime.setText(String.format("%1$d:%2$02d", Min, Sec));
	}

// resizeEvent was realized for PanelClient
//void resizeEvent(QResizeEvent *event)
//{
//		QMainWindow.resizeEvent(event);
//		TimerResize.stop();
//		TimerResize.start();

//				TimerTime.cancel();
//				TimerTime.schedule(TimerTimeTask, 0, 1000);

//		TimerResize.removeCallbacks(TimerResizeRunnable);
//		TimerResize.postDelayed(TimerResizeRunnable, 1000);

//}



	void TimerResizeTimer()
	{
		//		TimerResize.cancel();
		TimerResize.removeCallbacks(TimerResizeRunnable);

		long TimeFromLastResize_ms = System.currentTimeMillis() - LastResizeTime;

//	qDebug() << QString("TimerResizeTimer	") << QDateTime.currentDateTime().toString("mm:ss:zzz");

		if (TimeFromLastResize_ms > 1000)
		{
//			qDebug() << QString("AnimatePlaceTilesFast	") << TimeFromLastResize_ms;
			AnimatePlaceTilesFast();
			LastResizeTime = System.currentTimeMillis();
		}

	}

@Override
public void onBackPressed() {
	if (! ClosingAnimation)
	{
		ClosingAnimation = true;
		AnimateTilesDisappeare();
		return;
	}
	finish();
}

//-------------------------------   Animations   -----------------------------
	void CalcConsts()
	{
		int Height = (PanelClient.getMeasuredHeight());
		int Width = (PanelClient.getMeasuredWidth());

		if (Height > Width)
		{
			SpaceX = Math.round(((float)Width) / 20);
			TileSize = Math.round(((float)(Width - SpaceX * 2)) / Base);
			SpaceY = SpaceX + Math.round(((double)(Height - Width)) / 2);
		}
		else
		{
			SpaceY = Math.round(((float)Height) / 20);
			TileSize = Math.round(((float)(Height - SpaceY * 2)) / Base);
			SpaceX = SpaceY + Math.round(((float)(Width - Height)) / 2);
		}
		TileSpacing = Math.round(TileSize * 0.06);
		TileSize = Math.round(TileSize * 0.94);
		SpaceX = SpaceX + Math.round(((float)TileSpacing) / 2);
		SpaceY = SpaceY + Math.round(((float)TileSpacing) / 2);

	}

	void AnimatePlaceTilesFast()
	{
		CalcConsts();
		Log.d("Animate", "PlaceTilesFast");
		for (int i = 0; i < Tiles.length; i++)
			if (Tiles[i] != null)
			{
				Button Tile = Tiles[i];
				int delay = 30 * i; //delay for tile

				int Col = i % Base;
				int Row = i / Base;

				int width = Tile.getLayoutParams().width;
				int height = Tile.getLayoutParams().height;

				Log.d("Animate", String.format("width=%d, height=%d", width, height));

				float ScaleX = ((float)TileSize) / width;
				float ScaleY = ((float)TileSize) / height;

				float OffsetOnScaledTile = (TileSize - width) / 2.0f;

				long X = SpaceX + Math.round(Col * (width * ScaleX + TileSpacing) + OffsetOnScaledTile);
				long Y = SpaceY + Math.round(Row * (height *ScaleY + TileSpacing) + OffsetOnScaledTile);

//				Tile.animate().scaleX(ScaleX).scaleY(ScaleY)
//						.translationX(X).translationY(Y)
//						.setDuration(100).setStartDelay(100l + delay).setInterpolator(linear);
				Log.d("Animate", String.format("X=%d, Y=%d, ScaleX=%g, ScaleY=%g, ", X, Y, ScaleX, ScaleY));

				AnimateFloatDelay(Tile, "scaleX", ScaleX, 200, 200 + delay);
				AnimateFloatDelay(Tile, "scaleY", ScaleY, 200, 100 + delay);
				AnimateFloatDelay(Tile, "translationX", X, 200, delay);
				AnimateFloatDelay(Tile, "translationY", Y, 100, delay);
			}

	}

	void AnimateTilesDisappeare()
	{
		Button LastTile = null;
		for (int i = 0; i < Tiles.length; i++)
			if (Tiles[i] != null)
			{
				Button Tile = Tiles[i];
				int delay = 30 * i; //delay for tile

				long X = Math.round(Tile.getTranslationX() + ((TileSize) / 2.0));
				long Y = Math.round(Tile.getTranslationY() + TileSize) ;

				Tile.animate().scaleX(0.1f).scaleY(0.1f)
						.rotation(45.0f).alpha(0)
						.translationX(X).translationY(Y)
						.setDuration(400).setStartDelay(delay).setInterpolator(inBack);

//				AnimateFloatDelay(Tile, "scaleX", 0.1f, 400, delay);
//				AnimateFloatDelay(Tile, "scaleY", 0.1f, 400, delay);
//				AnimateFloatDelay(Tile, "rotation", 45, 400, delay);
//				AnimateFloatDelay(Tile, "translationY", Y, 400, delay, inBack);
//				AnimateFloatDelay(Tile, "translationX", X, 400, delay);
//				AnimateFloatDelay(Tile, "alpha", 0, 400, 100 + delay);

				LastTile = Tile;
			}

//		Log.d("ClosingAnimation", " = " + ((Boolean)ClosingAnimation).toString() +
//				" LastTile =" + ((LastTile == null)? "null": LastTile.toString())
//		);

		if (ClosingAnimation && (LastTile != null)) {

			LastTile.animate().setListener(new AnimatorListener() {
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

	void AnimatePrepareBeforePlace()
	{
		for (int i = 0; i < Tiles.length; i++)
			if (Tiles[i] != null) {
				Button Tile = Tiles[i];

				float ScaleX = ((float)TileSize) / Tile.getLayoutParams().width;
				float ScaleY = ((float)TileSize) / Tile.getLayoutParams().height;

				int Col = i % Base;
				int Row = i / Base;

				long X = SpaceX + Math.round(Col * (Tile.getLayoutParams().width * ScaleX + TileSpacing));
				long Y = SpaceY + Math.round(Row * (Tile.getLayoutParams().height * ScaleY + TileSpacing));

				Tile.setScaleX(0.5f);
				Tile.setScaleY(0.5f);

				Tile.setAlpha(0);
				Tile.setRotation(45.0f);
				Tile.setTranslationX(X + Math.round((TileSize) / 2.0));
				Tile.setTranslationY(Y + TileSize);

			}

		for (int i = 0; i < Tiles.length; i++)
			if (Tiles[i] != null) {
				Button Tile = Tiles[i];
				int delay = 30 * i; //delay for tile

//				Tile.animate().rotation(0).alpha(1)
//						.setDuration(200).setStartDelay(delay).setInterpolator(linear);

				AnimateFloatDelay(Tile, "rotation", 0, 400, delay);
				AnimateFloatDelay(Tile, "alpha", 1, 400, 100 + delay);
			}

	}

	void AnimateBaseNotChanged()
	{
		for (int i = 0; i < Tiles.length; i++)
			if (Tiles[i] != null) {
				Button Tile = Tiles[i];
				int delay = 30 * i; //delay for tile

				float OrigScaleX = Tile.getScaleX();
				float OrigScaleY = Tile.getScaleY();

				AnimateFloatDelay(Tile, "scaleX", OrigScaleX / 2.0f, 300, delay, inBack);
				AnimateFloatDelay(Tile, "scaleY", OrigScaleY / 2.0f, 300, delay, inBack);

				AnimateFloatDelay(Tile, "scaleX", OrigScaleX, 300, 350 + delay, outBack);
				AnimateFloatDelay(Tile, "scaleY", OrigScaleY, 300, 350 + delay, outBack);
			}
	}

	void AnimatePuzzleMatched(){
		for (int i = 0; i < Tiles.length; i++)
			if (Tiles[i] != null) {
				final Button Tile = Tiles[i];
				int delay = 30 * i; //delay for tile

				AnimateFloatDelay(Tile, "rotation", 360, 1000, 350, outBack);

//				Tile.setBackgroundTintList(ColorStateList.valueOf(/*lawngreen*/0xFF7CFC00));

				ValueAnimator colorAnimation = (ValueAnimator) Tile.getTag();
				int colorFrom = Tile.getBackgroundTintList().getDefaultColor();
				int colorTo = /*lawngreen*/0xFF7CFC00;
				colorAnimation.setObjectValues(colorFrom, colorTo);
				colorAnimation.setEvaluator(new ArgbEvaluator());
				colorAnimation.setDuration(1000);
				colorAnimation.setStartDelay(delay);
				colorAnimation.setRepeatCount(0);
				colorAnimation.start();
			}

	}

	void AnimateTimeRunningOut()
	{
		for (int i = 0; i < Tiles.length; i++)
			if (Tiles[i] != null) {
				final Button Tile = Tiles[i];

//				Tile.setBackgroundTintList(ColorStateList.valueOf(/*darkorange*/0xFFFF8C00));

				ValueAnimator colorAnimation = (ValueAnimator) Tile.getTag();
				int colorFrom = Tile.getBackgroundTintList().getDefaultColor();
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

	void AnimateTimeOver(){
		for (int i = 0; i < Tiles.length; i++)
			if (Tiles[i] != null) {
				final Button Tile = Tiles[i];
				int delay = 30 * i; //delay for tile
//				Tile.setBackgroundTintList(ColorStateList.valueOf(/*red*/0xFFFF0000));

				ValueAnimator colorAnimation = (ValueAnimator) Tile.getTag();

				int colorFrom = Tile.getBackgroundTintList().getDefaultColor();
				int colorTo = /*red*/0xFFFF0000;
				colorAnimation.setObjectValues(colorFrom, colorTo);
				colorAnimation.setEvaluator(new ArgbEvaluator());
				colorAnimation.setDuration(1000);
				colorAnimation.setStartDelay(delay);
				colorAnimation.setRepeatCount(0);
				colorAnimation.start();
			}

	}

	void AnimateNormalizeTilesColor(){
		for (int i = 0; i < Tiles.length; i++)
			if (Tiles[i] != null) {
				final Button Tile = Tiles[i];
				int delay = 30 * i; //delay for tile
//				Tile.setBackgroundTintList(ColorStateList.valueOf(TileFillNormalColor1));

				ValueAnimator colorAnimation = (ValueAnimator) Tile.getTag();
				int colorFrom = Tile.getBackgroundTintList().getDefaultColor();
				int colorTo = TileFillNormalColor1;
				colorAnimation.setObjectValues(colorFrom, colorTo);
				colorAnimation.setEvaluator(new ArgbEvaluator());
				colorAnimation.setDuration(1000);
				colorAnimation.setStartDelay(delay);
				colorAnimation.setRepeatCount(0);
				colorAnimation.start();
			}

	}

	void ShowDebug(){}

//-------------------------------  Test different Animations   -----------------------------

	public void ButtonDisappeareOnClick(View sender)
	{
		AnimateTilesDisappeare();
	}

	public void ButtonPlaceOnClick(View sender)
	{
		AnimateNormalizeTilesColor();
		AnimatePrepareBeforePlace();
		AnimatePlaceTilesFast();
	}

	public void ButtonTimeOverOnClick(View sender)
	{
		AnimateTimeOver();
	}

	public void ButtonTimeRunningOutOnClick(View sender)
	{
		AnimateTimeRunningOut();
	}

	public void ButtonPuzzleMatchedOnClick(View sender)
	{
		AnimatePuzzleMatched();
	}

//---------------------------  Realization of Property Animation   -----------------------------

	ObjectAnimator AnimateFloatDelayWait(View Target, String PropertyName,
	                                 float Value, long Duration_ms, long Delay_ms)
	{
		return AnimateFloatDelay(Target, PropertyName, Value, Duration_ms, Delay_ms, linear);
	}

	ObjectAnimator AnimateFloatDelay(View Target, String PropertyName,
	   float Value, long Duration_ms, long Delay_ms)
	{
		return AnimateFloatDelay(Target, PropertyName, Value, Duration_ms, Delay_ms, linear);
	}

	ObjectAnimator AnimateFloatDelay(View Target, String PropertyName,
      float Value, long Duration_ms, long Delay_ms,
      TimeInterpolator AInterpolator/*, boolean DeleteWhenStopped, boolean WaitAnimationEnd*/)
	{
		ObjectAnimator objectAnimator	= ObjectAnimator.ofFloat(Target, PropertyName, Value);
		objectAnimator.setDuration(Duration_ms);
		objectAnimator.setStartDelay(Delay_ms);
//		objectAnimator.setRepeatCount(1);
//		objectAnimator.setRepeatMode(ObjectAnimator.REVERSE);
		objectAnimator.setInterpolator(AInterpolator);
		objectAnimator.start();
		return objectAnimator;
	}

}
