package com.authcoinandroid.ui.activity;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.authcoinandroid.R;
import com.authcoinandroid.model.EntityIdentityRecord;
import com.authcoinandroid.module.messaging.ChallengeTypeMessageResponse;
import com.authcoinandroid.module.messaging.EvaluateChallengeMessage;
import com.authcoinandroid.module.messaging.EvaluateChallengeResponseMessage;
import com.authcoinandroid.module.messaging.SignatureMessage;
import com.authcoinandroid.module.messaging.SignatureResponseMessage;
import com.authcoinandroid.module.messaging.UserAuthenticatedMessage;
import com.authcoinandroid.module.messaging.VAProcessRunnable;
import com.authcoinandroid.service.challenge.ChallengeServiceImpl;
import com.authcoinandroid.service.transport.HttpRestAuthcoinTransport;
import com.authcoinandroid.service.transport.ServerInfo;
import com.authcoinandroid.ui.AuthCoinApplication;
import com.authcoinandroid.ui.fragment.authentication.AuthenticationSuccessfulFragment;
import com.authcoinandroid.ui.fragment.authentication.ChallengeTypeSelectorFragment;
import com.authcoinandroid.ui.fragment.authentication.EirSelectorFragment;
import com.authcoinandroid.ui.fragment.authentication.EvaluateChallengeFragment;
import com.authcoinandroid.ui.fragment.authentication.SignatureFragment;

public class AuthenticationActivity extends AppCompatActivity {

    private Handler mainThreadHandler;
    private Handler vaThreadHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Uri uri = getIntent().getData();

        String demoServer = uri.getQueryParameter("serverUrl");
        ChallengeServiceImpl challengeService = ((AuthCoinApplication) getApplication()).getChallengeService();
        HttpRestAuthcoinTransport transport =
                new HttpRestAuthcoinTransport(demoServer, challengeService);
        EirSelectorFragment eirSelectionFragment = new EirSelectorFragment();
        eirSelectionFragment.setNextButtonListener(v -> {
            EntityIdentityRecord selectedEir = eirSelectionFragment.getSelectedEir();

            new Thread(() -> {
                ServerInfo serverInfo = transport.start();
                EntityIdentityRecord target = ((AuthCoinApplication) getApplication()).getIdentityService().getEirById(serverInfo.getServerEir()).blockingSingle();
                ((AuthCoinApplication) getApplication()).getEirRepository().save(target).blockingGet();
                vaThreadHandler.post(
                        new VAProcessRunnable(
                                mainThreadHandler,
                                selectedEir,
                                target,
                                transport,
                                challengeService,
                                ((AuthCoinApplication) getApplication()).getWalletService()
                        )
                );
            }).start();
        });

        transaction.replace(R.id.container, eirSelectionFragment).commit();

        HandlerThread handlerThread = new HandlerThread("VA");
        handlerThread.start();

        mainThreadHandler = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(Message msg) {
                Log.e("MT", "Main thread: " + Thread.currentThread().getName());
                switch (msg.what) {
                    case 1:
                        // challenge type
                        ChallengeTypeSelectorFragment selectionFragment = new ChallengeTypeSelectorFragment();
                        selectionFragment.setSelectChallengeButtonListener(
                                v -> {
                                    synchronized (VAProcessRunnable.lock) {
                                        VAProcessRunnable.queue.add(new ChallengeTypeMessageResponse(selectionFragment.getSelectedChallenge()));
                                        VAProcessRunnable.lock.notify();
                                    }
                                }
                        );
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.container, selectionFragment).commit();
                        break;

                    case 2:
                        // evaluate target's challenge sent to verifier
                        EvaluateChallengeFragment evaluateChallengeFragment = new EvaluateChallengeFragment();
                        evaluateChallengeFragment.setChallengeRecord(((EvaluateChallengeMessage) msg.obj).getChallenge());
                        evaluateChallengeFragment.setApproveButtonListener(
                                v -> {
                                    synchronized (VAProcessRunnable.lock) {
                                        VAProcessRunnable.queue.add(new EvaluateChallengeResponseMessage(evaluateChallengeFragment.isApproved()));
                                        VAProcessRunnable.lock.notify();
                                    }
                                }
                        );
                        startFragment(evaluateChallengeFragment);
                        break;
                    case 3:
                        SignatureFragment signatureFragment = new SignatureFragment();
                        signatureFragment.setChallengeResponse(((SignatureMessage) msg.obj).getChallengeResponse());
                        signatureFragment.setApproveSignatureListener(
                                v -> {
                                    synchronized (VAProcessRunnable.lock) {
                                        VAProcessRunnable.queue.add(new SignatureResponseMessage(signatureFragment.getLifespan(), true));
                                        VAProcessRunnable.lock.notify();
                                    }
                                }
                        );
                        startFragment(signatureFragment);
                        break;
                    case 10:
                        AuthenticationSuccessfulFragment asf = new AuthenticationSuccessfulFragment();
                        asf.setResult(((UserAuthenticatedMessage) msg.obj));
                        startFragment(asf);
                        break;
                }
            }
        };

        vaThreadHandler = new Handler(handlerThread.getLooper());
    }

    private void startFragment(Fragment f) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, f).commit();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Hide keyboard and lose focus on EditText when clicking anywhere outside
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

}
