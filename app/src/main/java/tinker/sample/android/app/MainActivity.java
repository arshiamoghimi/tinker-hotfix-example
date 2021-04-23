/*
 * Tencent is pleased to support the open source community by making Tinker available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tinker.sample.android.app;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.downloader.Error;
import com.downloader.OnCancelListener;
import com.downloader.OnDownloadListener;
import com.downloader.OnPauseListener;
import com.downloader.OnProgressListener;
import com.downloader.OnStartOrResumeListener;
import com.downloader.PRDownloader;
import com.downloader.Progress;
import com.downloader.Status;
import com.tencent.tinker.lib.library.TinkerLoadLibrary;
import com.tencent.tinker.lib.service.AbstractResultService;
import com.tencent.tinker.lib.tinker.Tinker;
import com.tencent.tinker.lib.tinker.TinkerInstaller;
import com.tencent.tinker.lib.util.TinkerLog;
import com.tencent.tinker.loader.shareutil.ShareConstants;
import com.tencent.tinker.loader.shareutil.ShareTinkerInternals;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;
import tinker.sample.android.R;
import tinker.sample.android.eventsmodel.MessageEvent;
import tinker.sample.android.models.ConfigResponseObject;
import tinker.sample.android.servercom.CustomCallBack;
import tinker.sample.android.servercom.GetDataService;
import tinker.sample.android.servercom.RetrofitClientInstance;
import tinker.sample.android.util.AppConstant;
import tinker.sample.android.util.UserPrefs;
import tinker.sample.android.util.Utils;

public class MainActivity extends AppCompatActivity {
    int downloadIdOne;
    String patchUrl = "";

    ConfigResponseObject responseObject = null;

    Button bugTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        askForRequiredPermissions();

        Button cleanPatchButton = findViewById(R.id.cleanPatch);

        cleanPatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Tinker.with(getApplicationContext()).cleanPatch();
            }
        });

        Button killSelfButton = findViewById(R.id.killSelf);

        killSelfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareTinkerInternals.killAllOtherProcess(getApplicationContext());
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });

        bugTest = findViewById(R.id.bug_test);

        bugTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<String> strings = new ArrayList<>();
                strings.get(0);

//                Toast.makeText(MainActivity.this, "Bug Fixed", Toast.LENGTH_SHORT).show();
//                bugTest.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            }
        });
    }

    private void askForRequiredPermissions() {
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
    }

    private boolean hasRequiredPermissions() {
        final int res;
        if (Build.VERSION.SDK_INT >= 16) {
            res = ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            res = ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return res == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setBackground(false);

        if (hasRequiredPermissions()) {
            fetchConfig();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Utils.setBackground(true);
    }

    private void patchDownloader(final String patchUrl) {
        if (Status.RUNNING == PRDownloader.getStatus(downloadIdOne)) {
            PRDownloader.pause(downloadIdOne);
            return;
        }

        if (Status.PAUSED == PRDownloader.getStatus(downloadIdOne)) {
            PRDownloader.resume(downloadIdOne);
            return;
        }

        final ProgressDialog progressBarDialog = new ProgressDialog(MainActivity.this);
        progressBarDialog.setTitle("Patch Downloading...");
        progressBarDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressBarDialog.setCanceledOnTouchOutside(false);
        progressBarDialog.setIndeterminate(true);

        progressBarDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Background", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,
                                int whichButton) {
                showToast("Downloading continue in background");
            }
        });

        progressBarDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                PRDownloader.cancel(downloadIdOne);
            }
        });

        progressBarDialog.setProgress(0);
        progressBarDialog.show();

        downloadIdOne = PRDownloader.download(patchUrl, Utils.getRootDirPath(MainActivity.this), "patch_signed_7zip.apk")
                .build()
                .setOnStartOrResumeListener(new OnStartOrResumeListener() {
                    @Override
                    public void onStartOrResume() {

                        progressBarDialog.setIndeterminate(false);
                    }
                })
                .setOnPauseListener(new OnPauseListener() {
                    @Override
                    public void onPause() {

                    }
                })
                .setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel() {
                        downloadIdOne = 0;
                        progressBarDialog.setProgress(0);
                        progressBarDialog.dismiss();

                    }
                })
                .setOnProgressListener(new OnProgressListener() {
                    @Override
                    public void onProgress(Progress progress) {
                        long progressPercent = progress.currentBytes * 100 / progress.totalBytes;
                        progressBarDialog.setProgress((int) progressPercent);
                    }
                })
                .start(new OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        showToast("Patch Download Complete");
                        progressBarDialog.dismiss();
                        loadPatch();
                    }

                    @Override
                    public void onError(Error error) {
                        showToast("Download Error! something went wrong try again, try again.");
                        downloadIdOne = 0;
                        progressBarDialog.setProgress(0);
                        progressBarDialog.dismiss();
                        deleteErrorFile(patchUrl);
                    }

                });
    }

    private void deleteErrorFile(String path) {
        File file = new File(getExternalFilesDirs(null)[0], getPatchFilePath(path) + ".temp");
        if (file.exists()) {
            file.delete();
            if (file.exists()) {
                showToast("Not clear");
            } else {
                showToast("Session clear");
            }
        }
    }

    private String getPatchFilePath(String videoURl) {
        Uri uri = Uri.parse(videoURl);
        return uri.getLastPathSegment();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void fetchConfig() {
        GetDataService service = RetrofitClientInstance.getRetrofitInstance().create(GetDataService.class);
        Call<ConfigResponseObject> call = service.getConfig();
        call.enqueue(new CustomCallBack<ConfigResponseObject>(MainActivity.this) {
            @Override
            public void onResponse(Call<ConfigResponseObject> call, Response<ConfigResponseObject> response) {
                super.onResponse(call, response);
                if (response.body() != null) {
                    responseObject = response.body();
                    if (responseObject.isPatchAvailable()) {
                        if (responseObject.getPatchId().equals(UserPrefs.getInstance(MainActivity.this).getString(AppConstant.PATCH_ID, ""))) {
                            showToast("Patch Already Applied!");
                        } else {
                            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                            alertDialog.setTitle("Patch Available");
                            alertDialog.setMessage("Want to download patch?");
                            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Download",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            patchUrl = responseObject.getPatchUrl();
                                            patchDownloader(responseObject.getPatchUrl());
                                        }
                                    });
                            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Later",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();
                        }
                    } else {
                        showToast("Patch not available!");
                    }
                }
            }

            @Override
            public void onFailure(Call<ConfigResponseObject> call, Throwable t) {
                super.onFailure(call, t);
                Toast.makeText(MainActivity.this, "Something went wrong...Please try later!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean checkPatchExist() {
        File file = new File(getExternalFilesDirs(null)[0], "patch_signed_7zip.apk");
        return file.exists();
    }

    private void loadPatch() {
        if (checkPatchExist()) {
            showToast("Patch applying...");
            File file = new File(getExternalFilesDirs(null)[0], "patch_signed_7zip.apk");
            TinkerInstaller.onReceiveUpgradePatch(getApplicationContext(), file.getAbsolutePath());
        } else {
            showToast("Something wrong, patch file not found!");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
}
