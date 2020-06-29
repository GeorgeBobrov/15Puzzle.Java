package com.example.a15puzzle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.os.Bundle;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

	enum TMode {Game,
		GameOver,
		JustShuffled,
		PuzzleMatched };

//	public class Button extends androidx.appcompat.widget.AppCompatButton {
//		public Button(Context context) {
//			super(context);
//		}
//	}

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		PanelClient = (RelativeLayout) findViewById(R.id.PanelClient);
//		mStartButton = (Button) findViewById(R.id.buttonStart);
		TextTime = (TextView) findViewById(R.id.TextTime);

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

		TileFillNormalColor1 = Color.parseColor("#FFFFE4C4"); //bisque
		TileFillNormalColor2 = Color.parseColor("#FFABE024");

//  PanelClient  background-color: rgb(244, 244, 244);

		RandomGen = new Random();
		SetBase(4);

	}

	void  SetMode(TMode Value)
	{
		Mode = Value;
		if (Mode == TMode.Game) {
			TimerTime.postDelayed(TimerTimeRunnable, 1000);
			Log.d("Timer", "TimerTime.postDelayed(TimerTimeRunnable, 1000)");
		}
		else {
			TimerTime.removeCallbacks(TimerTimeRunnable);
			Log.d("Timer", "TimerTime.removeCallbacks(TimerTimeRunnable)");
		}
/*
		// Param is optional, to run task on UI thread.
		Handler handler = new Handler(Looper.getMainLooper());
		Runnable runnable = new Runnable() { @Override public void run() { }};
		// Do the task...
		handler.postDelayed(this, milliseconds);
		// Optional, to repeat the task. } };
		handler.postDelayed(runnable, milliseconds);
		// Stop a repeating task like this.
		handler.removeCallbacks(runnable);
*/

//		TimerTime.schedule(TimerTimeTask, 0, 1000);
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

		TimerCreateTiles.postDelayed(TimerCreateTilesRunnable, 100);
	}



	void TimerCreateTilesTimer()
	{
		CreateTiles();
		AnimatePrepareBeforePlace();
		AnimatePlaceTilesFast();
	}



	void CreateTiles()
	{
		if (Tiles != null)
		for (int i = 0; i < Tiles.length; i++)
			if (Tiles[i] != null)
			{
//				TGradientAnimation *GradientAni = (TGradientAnimation*)Tiles[i].property("GradientAni").value<void *>() ;
				Tiles[i] = null;
			}

		Tiles = new Button[Base * Base];
		for (int i = 0; i < Tiles.length - 1; i++)
			if (Tiles[i] == null)
			{
				Button NewTile;

				NewTile = (Button) new Button(this);

				NewTile.setOnClickListener(TileClickListener);

				NewTile.setText(String.valueOf(i + 1));

//				TGradientAnimation *GradientAni = new TGradientAnimation(NewTile, GenerateTileStyleSheet);
//				NewTile.setProperty("GradientAni", qVariantFromValue((void*) GradientAni));
//
//				NewTile.setStyleSheet(GenerateTileStyleSheet(TileFillNormalColor1, TileFillNormalColor2));
//				GradientAni.SetCurColors(TileFillNormalColor1, TileFillNormalColor2);

//				NewTile.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
//						LayoutParams.WRAP_CONTENT));

				NewTile.setHeight(50);
				NewTile.setWidth(50);

				PanelClient.addView(NewTile);
//				NewTile.layout();

				NewTile.setTranslationX((i/4)*50);
				NewTile.setTranslationY((i%4)*50);

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

		long X = SpaceX + Math.round(NewCol * (TileSize + TileSpacing));
		long Y = SpaceY + Math.round(NewRow * (TileSize + TileSpacing));

//		if (MoveAniDuration > 0)
//		{
//			AnimatePropertyDelay(ATile, "geometry", geometry, MoveAniDuration, 0, QEasingCurve.OutExpo, true, WaitAnimationEnd);
//		}
//		else
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
				int TextNumber = Integer.parseInt( Tiles[i].getText().toString() );

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



	public void OnButtonShuffleClicked(View sender)
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
				WasMoved =  TryMoveTile(NewI, MoveAniDuration, true);
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

//	void closeEvent(QCloseEvent event)
//	{
//		if (! ClosingAnimation)
//		{
//			AnimateTilesDisappeare();
//			ClosingAnimation = true;
//		}
//
//	}

//-------------------------------   Animations   -----------------------------
	void CalcConsts( ){
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

	void AnimatePlaceTilesFast( ){
		CalcConsts();

		for (int i = 0; i < Tiles.length; i++)
			if (Tiles[i] != null)
			{
				float ScaleX = ((float)TileSize) / Tiles[i].getMinimumWidth();
				float ScaleY = ((float)TileSize) / Tiles[i].getMinimumHeight();

//				int OffsetOnScaledTile = (TileSize - Tiles[i].getWidth()) / 2;

				int dl = 30 * i; //delay for tile

//				Tiles[i].style.transition = `all 200ms linear 0ms,
//				transform 200ms linear ${100 + dl}ms,
//				left 200ms linear ${dl}ms,
//				top 100ms linear ${dl}ms,
//				opacity 400ms linear ${100 + dl}ms`;

//				ScaleSettings = "scale(" + ScaleX + ")";
//				Tiles[i].style.transform = ScaleSettings;
				Tiles[i].setScaleX(ScaleX);
				Tiles[i].setScaleY(ScaleY);



				int Col = i % Base;
				int Row = i / Base;

				long X = SpaceX + Math.round(Col * (TileSize + TileSpacing));
				long Y = SpaceY + Math.round(Row * (TileSize + TileSpacing));


//				let X = SpaceX + Math.round(Col * (Tiles[i].offsetWidth * ScaleX + TileSpacing) + OffsetOnScaledTile);
//				let Y = SpaceY + Math.round(Row * (Tiles[i].offsetHeight * ScaleY + TileSpacing) + OffsetOnScaledTile);

				Tiles[i].setTranslationX(X);
				Tiles[i].setTranslationY(Y);

			}

	}

	void AnimateTilesDisappeare( ){}
	void AnimatePrepareBeforePlace( ){}
	void AnimateBaseNotChanged( ){}
	void AnimateTimeRunningOut( ){}
	void AnimatePuzzleMatched( ){}
	void AnimateTimeOver( ){}
	void AnimateNormalizeTilesColor( ){}
	void ShowDebug( ){}

}
