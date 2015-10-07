package rs.akcelerometarapp.utils;

import android.app.ProgressDialog;
import android.content.Context;

import rs.akcelerometarapp.R;

/**
 * Created by RADEEE on 07-Oct-15.
 */
public class ProgressDialogUtils {

    public static ProgressDialog initProgressDialog(Context context) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(context.getString(R.string.progress_dialog_text));
        progressDialog.setCanceledOnTouchOutside(false);
        return progressDialog;
    }

    public static void dismissProgressDialog(ProgressDialog progressDialog) {
        if ((progressDialog != null) && (progressDialog.isShowing())) {
            try {
                progressDialog.dismiss();
            } catch (Throwable thr) {}
        }
    }

    public static void showProgressDialog(ProgressDialog progressDialog) {
        if ((progressDialog != null) && (!progressDialog.isShowing())) {
            progressDialog.show();
        }
    }

}

