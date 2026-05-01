package com.recipebookpro.presentation.ui.book;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

/**
 * Gelişmiş 3D Sayfa Çevirme (Book Flip) efekti. | Advanced 3D Page Flip effect.
 * Normal kaydırma yerine gerçek bir kitabın yaprakları gibi döner. | Turns like real book pages instead of normal scrolling.
 */
public class CurlPageTransformer implements ViewPager2.PageTransformer {

    @Override
    public void transformPage(@NonNull View page, float position) {
        // Kamera uzaklığını artırarak perspektifi düzeltiyoruz | Improving perspective by increasing camera distance
        page.setCameraDistance(20000);

        if (position < -1) { // [-Infinity, -1) Sayfa tamamen solda | Page completely on the left
            page.setAlpha(0f);
        } else if (position <= 0) { // [-1, 0] Mevcut sayfa (sola doğru gidiyor) | Current page (moving left)
            page.setAlpha(1f);
            page.setPivotX(page.getWidth()); // Merkez sağ kenar | Pivot at right edge
            page.setPivotY(page.getHeight() * 0.5f);
            page.setRotationY(-90 * Math.abs(position));
            
            // Sayfanın normal ViewPager kaymasını iptal edip olduğu yerde dönmesini sağlıyoruz | Cancel normal scrolling and make it rotate in place
            page.setTranslationX(page.getWidth() * -position);
            
        } else if (position <= 1) { // (0, 1] Sonraki sayfa (sağdan geliyor) | Next page (coming from right)
            page.setAlpha(1f);
            page.setPivotX(0); // Merkez sol kenar | Pivot at left edge
            page.setPivotY(page.getHeight() * 0.5f);
            page.setRotationY(90 * Math.abs(position));
            
            // Sayfanın normal ViewPager kaymasını iptal edip olduğu yerde dönmesini sağlıyoruz | Cancel normal scrolling and make it rotate in place
            page.setTranslationX(page.getWidth() * -position);
            
        } else { // (1, +Infinity] Sayfa tamamen sağda | Page completely on the right
            page.setAlpha(0f);
        }
    }
}
