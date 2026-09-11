package com.example.pdfreader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends Activity {

    private static final int OPEN_PDF_REQUEST = 1001;

    private LinearLayout pdfContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button openPdfButton = findViewById(R.id.openPdfButton);
        pdfContainer = findViewById(R.id.pdfContainer);

        openPdfButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            startActivityForResult(intent, OPEN_PDF_REQUEST);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OPEN_PDF_REQUEST &&
                resultCode == RESULT_OK &&
                data != null &&
                data.getData() != null) {

            renderPdf(data.getData());
        }
    }

    private void renderPdf(Uri uri) {
        pdfContainer.removeAllViews();

        try {
            ParcelFileDescriptor fileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");

            if (fileDescriptor == null) {
                Toast.makeText(this, "Cannot open PDF", Toast.LENGTH_SHORT).show();
                return;
            }

            PdfRenderer renderer = new PdfRenderer(fileDescriptor);

            for (int i = 0; i < renderer.getPageCount(); i++) {
                PdfRenderer.Page page = renderer.openPage(i);

                int width = page.getWidth() * 2;
                int height = page.getHeight() * 2;

                Bitmap bitmap = Bitmap.createBitmap(
                        width,
                        height,
                        Bitmap.Config.ARGB_8888
                );

                page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                );

                ImageView imageView = new ImageView(this);
                imageView.setAdjustViewBounds(true);
                imageView.setImageBitmap(bitmap);

                pdfContainer.addView(
                        imageView,
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                );

                page.close();
            }

            renderer.close();
            fileDescriptor.close();

        } catch (IOException e) {
            Toast.makeText(this, "Failed to open PDF", Toast.LENGTH_SHORT).show();
        }
    }
}
