package com.recipebookpro.presentation.ui.book;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

/**
 * GeliÅŸmiÅŸ 3D Sayfa Ã‡evirme (Book Flip) efekti.
 * Normal kaydÄ±rma yerine gerÃ§ek bir kitabÄ±n yapraklarÄ± gibi dÃ¶ner.
 */
public class CurlPageTransformer implements ViewPager2.PageTransformer {

    @Override
    public void transformPage(@NonNull View page, float position) {
        // Kamera uzaklÄ±ÄŸÄ±nÄ± artÄ±rarak perspektifi dÃ¼zeltiyoruz
        page.setCameraDistance(20000);

        if (position < -1) { // [-Infinity, -1) Sayfa tamamen solda
            page.setAlpha(0f);
        } else if (position <= 0) { // [-1, 0] Mevcut sayfa (sola doÄŸru gidiyor)
            page.setAlpha(1f);
            page.setPivotX(page.getWidth()); // Merkez saÄŸ kenar
            page.setPivotY(page.getHeight() * 0.5f);
            page.setRotationY(-90 * Math.abs(position));
            
            // SayfanÄ±n normal ViewPager kaymasÄ±nÄ± iptal edip olduÄŸu yerde dÃ¶nmesini saÄŸlÄ±yoruz
            page.setTranslationX(page.getWidth() * -position);
            
        } else if (position <= 1) { // (0, 1] Sonraki sayfa (saÄŸdan geliyor)
            page.setAlpha(1f);
            page.setPivotX(0); // Merkez sol kenar
            page.setPivotY(page.getHeight() * 0.5f);
            page.setRotationY(90 * Math.abs(position));
            
            // SayfanÄ±n normal ViewPager kaymasÄ±nÄ± iptal edip olduÄŸu yerde dÃ¶nmesini saÄŸlÄ±yoruz
            page.setTranslationX(page.getWidth() * -position);
            
        } else { // (1, +Infinity] Sayfa tamamen saÄŸda
            page.setAlpha(0f);
        }
    }
}
