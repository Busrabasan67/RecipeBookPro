package com.recipebookpro.presentation.ui.recipe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.ChipGroup;
import android.widget.ProgressBar;
import com.recipebookpro.R;

import java.util.ArrayList;
import java.util.List;

public class StickerSelectorBottomSheet extends BottomSheetDialogFragment {

    public interface OnStickerSelectedListener {
        void onStickerSelected(String imageUrl);
    }

    private static List<StickerModel> cachedStickers = null;
    private OnStickerSelectedListener listener;

    private static class StickerModel {
        String url;
        String category;
        StickerModel(String url, String category) {
            this.url = url;
            this.category = category;
        }
    }

    public void setOnStickerSelectedListener(OnStickerSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_sticker_selector, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rvStickers);
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupCategories);
        
        rv.setLayoutManager(new GridLayoutManager(getContext(), 3));
        ProgressBar progressBar = view.findViewById(R.id.progressStickers);

        fetchAndShowStickers(rv, progressBar, "all");

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            String category = "all";
            if (id == R.id.chipDessert) category = "dessert";
            else if (id == R.id.chipDrink) category = "drink";
            else if (id == R.id.chipFood) category = "food";
            else if (id == R.id.chipOrnamental) category = "ornamental";
            else if (id == R.id.chipTools) category = "tools_equipment";
            else if (id == R.id.chipVegetable) category = "vegetable";

            filterStickers(rv, category);
        });
    }

    private void filterStickers(RecyclerView rv, String category) {
        if (cachedStickers == null) return;
        List<String> filteredUrls = new ArrayList<>();
        for (StickerModel s : cachedStickers) {
            if (category.equals("all") || s.category.equalsIgnoreCase(category)) {
                filteredUrls.add(s.url);
            }
        }
        rv.setAdapter(new StickerAdapter(filteredUrls, imageUrl -> {
            if (listener != null) listener.onStickerSelected(imageUrl);
            dismiss();
        }));
    }

    private void fetchAndShowStickers(RecyclerView rv, ProgressBar progressBar, String initialCategory) {
        View view = getView();
        android.widget.TextView tvStatus = view != null ? view.findViewById(R.id.tvStickerStatus) : null;
        
        if (cachedStickers != null && !cachedStickers.isEmpty()) {
            if (tvStatus != null) tvStatus.setVisibility(View.GONE);
            filterStickers(rv, initialCategory);
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvStatus != null) {
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText("Stickerlar yükleniyor...");
            tvStatus.setOnClickListener(null); // Remove previous click listener
        }

        new Thread(() -> {
            List<StickerModel> stickers = new ArrayList<>();
            String detectedBranch = "main";
            String errorMsg = null;
            int retryCount = 3;
            
            while (retryCount > 0 && stickers.isEmpty()) {
                try {
                    // 1. Dynamic Branch Detection
                    java.net.URL repoUrl = new java.net.URL("https://api.github.com/repos/zeyynepp0/PhotoshopExtension_Images");
                    java.net.HttpURLConnection repoConn = (java.net.HttpURLConnection) repoUrl.openConnection();
                    repoConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36");
                    repoConn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                    repoConn.setConnectTimeout(8000);
                    repoConn.setReadTimeout(8000);

                    int code = repoConn.getResponseCode();
                    if (code == 200) {
                        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(repoConn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        org.json.JSONObject repoJson = new org.json.JSONObject(sb.toString());
                        detectedBranch = repoJson.optString("default_branch", "main");
                        repoConn.disconnect();

                        // 2. Recursive Tree Fetch
                        java.net.URL treeUrl = new java.net.URL("https://api.github.com/repos/zeyynepp0/PhotoshopExtension_Images/git/trees/" + detectedBranch + "?recursive=1");
                        java.net.HttpURLConnection treeConn = (java.net.HttpURLConnection) treeUrl.openConnection();
                        treeConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36");
                        treeConn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                        treeConn.setConnectTimeout(15000);

                        if (treeConn.getResponseCode() == 200) {
                            java.io.BufferedReader tbr = new java.io.BufferedReader(new java.io.InputStreamReader(treeConn.getInputStream()));
                            StringBuilder tsb = new StringBuilder();
                            while ((line = tbr.readLine()) != null) tsb.append(line);
                            
                            org.json.JSONObject root = new org.json.JSONObject(tsb.toString());
                            org.json.JSONArray tree = root.optJSONArray("tree");
                            
                            if (tree != null) {
                                for (int i = 0; i < tree.length(); i++) {
                                    org.json.JSONObject obj = tree.getJSONObject(i);
                                    String path = obj.optString("path", "");
                                    if ("blob".equals(obj.optString("type")) && (path.toLowerCase().endsWith(".png") || path.toLowerCase().endsWith(".webp"))) {
                                        String[] parts = path.split("/");
                                        if (parts.length >= 2) {
                                            String downloadUrl = "https://raw.githubusercontent.com/zeyynepp0/PhotoshopExtension_Images/" + detectedBranch + "/" + path;
                                            stickers.add(new StickerModel(downloadUrl, parts[0]));
                                        }
                                    }
                                }
                            }
                            errorMsg = null;
                        } else {
                            errorMsg = "Liste hatası: " + treeConn.getResponseCode();
                        }
                        treeConn.disconnect();
                    } else if (code == 403) {
                        errorMsg = "GitHub Limitine Takıldı (403). Lütfen bekleyin.";
                        retryCount = 0; // Don't retry on 403
                    } else {
                        errorMsg = "Sunucu Hatası: " + code;
                    }
                    repoConn.disconnect();

                } catch (java.net.UnknownHostException e) {
                    errorMsg = "İnternet Bağlantısı Yok veya GitHub'a Erişilemiyor.";
                } catch (Exception e) {
                    errorMsg = "Bağlantı Hatası: " + e.getMessage();
                }

                if (stickers.isEmpty() && retryCount > 0) {
                    retryCount--;
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
            }

            if (!stickers.isEmpty()) {
                cachedStickers = stickers;
                errorMsg = null;
            }

            final String finalError = errorMsg;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (tvStatus != null) {
                        if (finalError != null) {
                            tvStatus.setText(finalError + "\n(Tekrar denemek için dokunun)");
                            tvStatus.setVisibility(View.VISIBLE);
                            tvStatus.setOnClickListener(v -> fetchAndShowStickers(rv, progressBar, initialCategory));
                        } else {
                            tvStatus.setVisibility(View.GONE);
                        }
                    }
                    filterStickers(rv, initialCategory);
                });
            }
        }).start();
    }
}
