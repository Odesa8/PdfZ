package com.example.pdfreader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends Activity {

    private static final int OPEN_PDF = 1001;

    private ImageView imageView;
    private TextView pageInfo;
    private PdfRenderer renderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor fileDescriptor;
    private int pageIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        Button openButton = new Button(this);
        openButton.setText("Open PDF");

        imageView = new ImageView(this);
        imageView.setAdjustViewBounds(true);

        LinearLayout controls = new LinearLayout(this);

        Button previous = new Button(this);
        previous.setText("Previous");

        pageInfo = new TextView(this);
        pageInfo.setText("No PDF");

        Button next = new Button(this);
        next.setText("Next");

        controls.addView(previous);
        controls.addView(pageInfo);
        controls.addView(next);

        root.addView(openButton);
        root.addView(
                imageView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );
        root.addView(controls);

        setContentView(root);

        openButton.setOnClickListener(v -> openPdfPicker());

        previous.setOnClickListener(v -> {
            if (renderer != null && pageIndex > 0) {
                showPage(pageIndex - 1);
            }
        });

        next.setOnClickListener(v -> {
            if (renderer != null && pageIndex < renderer.getPageCount() - 1) {
                showPage(pageIndex + 1);
            }
        });

        Uri uri = getIntent().getData();
        if (uri != null) {
            openPdf(uri);
        }
    }

    private void openPdfPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, OPEN_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OPEN_PDF &&
                resultCode == RESULT_OK &&
                data != null &&
                data.getData() != null) {

            openPdf(data.getData());
        }
    }

    private void openPdf(Uri uri) {
        closePdf();

        try {
            fileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");

            if (fileDescriptor == null) {
                return;
            }

            renderer = new PdfRenderer(fileDescriptor);
            pageIndex = 0;
            showPage(pageIndex);

        } catch (IOException e) {
            pageInfo.setText("Cannot open PDF");
        }
    }

    private void showPage(int index) {
        if (renderer == null) {
            return;
        }

        if (currentPage != null) {
            currentPage.close();
        }

        currentPage = renderer.openPage(index);

        int width = currentPage.getWidth() * 2;
        int height = currentPage.getHeight() * 2;

        Bitmap bitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
        );

        currentPage.render(
                bitmap,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
        );

        imageView.setImageBitmap(bitmap);
        pageIndex = index;

        pageInfo.setText(
                (pageIndex + 1) + " / " + renderer.getPageCount()
        );
    }

    private void closePdf() {
        if (currentPage != null) {
            currentPage.close();
            currentPage = null;
        }

        if (renderer != null) {
            renderer.close();
            renderer = null;
        }

        if (fileDescriptor != null) {
            try {
                fileDescriptor.close();
            } catch (IOException ignored) {
            }
            fileDescriptor = null;
        }
    }

    @Override
    protected void onDestroy() {
        closePdf();
        super.onDestroy();
    }
}
