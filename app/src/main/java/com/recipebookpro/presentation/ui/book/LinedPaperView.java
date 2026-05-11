package com.recipebookpro.presentation.ui.book;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class LinedPaperView extends FrameLayout {
    private Paint linePaint;
    private Paint marginPaint;
    private Paint holesPaint;
    private final int lineHeight = 90;

    public LinedPaperView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setBackgroundColor(0xFFFDFBF7); // Kirli beyaz / krem kağıt rengi | Off-white / cream paper color

        linePaint = new Paint();
        linePaint.setColor(0x440066FF); // Soluk mavi çizgiler | Faint blue lines
        linePaint.setStrokeWidth(3f);
        
        marginPaint = new Paint();
        marginPaint.setColor(0x66FF0000); // Kırmızı marjin çizgisi | Red margin line
        marginPaint.setStrokeWidth(5f);

        holesPaint = new Paint();
        holesPaint.setColor(0xFF222222); // Delik rengi (koyu gri/siyah) | Hole color (dark gray/black)
        holesPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        int marginX = 60;

        // Defter deliklerini çiz (Sol tarafa, spiral hissiyatı vermek için) | Draw notebook holes (left side, for spiral feel)
        for (int y = 100; y < height; y += 120) {
            canvas.drawCircle(25, y, 12, holesPaint);
        }

        // Yatay mavi çizgiler | Horizontal blue lines
        for (int y = 200; y < height; y += lineHeight) {
            canvas.drawLine(0, y, width, y, linePaint);
        }
        
        // Dikey kırmızı marjin | Vertical red margin
        canvas.drawLine(marginX, 0, marginX, height, marginPaint);
    }
}
