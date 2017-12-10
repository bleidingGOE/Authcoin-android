package com.authcoinandroid.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.authcoinandroid.R;
import com.authcoinandroid.task.WalletCreationTask;
import com.authcoinandroid.ui.activity.MainActivity;
import com.authcoinandroid.util.AndroidUtil;

public class WelcomeFragment extends Fragment {
    private final static String LOG_TAG = "WelcomeFragment";

    @BindView(R.id.et_wallet_password)
    EditText walletPassword;

    @BindView(R.id.et_wallet_password_confirm)
    EditText walletPasswordConfirm;

    @BindView(R.id.pb_wallet_creation)
    ProgressBar progressBar;

    @BindView(R.id.btn_create_wallet)
    Button createWalletButton;

    public WelcomeFragment() {
    }

    @OnClick({R.id.btn_create_wallet})
    void onCreateWalletClick(View view) {
        if (validatePasswords()) {
            disableElements();
            String password = walletPassword.getText().toString();
            new WalletCreationTask(getContext(), password, result -> {
                if (result.getError() != null) {
                    enableElements();
                    AndroidUtil.displayNotification(getContext(), "Failed to create wallet");
                } else {
                    clearInputFields();
                    Log.d(LOG_TAG, "Created wallet, mnemonic code: " + result.getResult().getKeyChainSeed().getMnemonicCode());
                    AndroidUtil.displayNotification(getContext(), "Wallet created");
                    ((MainActivity) getActivity()).applyFragment(IdentityFragment.class, false, false);
                }
            }).execute();
        }
    }

    private boolean validatePasswords() {
        String password = walletPassword.getText().toString();
        String passwordConfirm = walletPasswordConfirm.getText().toString();

        if (!password.equals(passwordConfirm)) {
            Log.d(LOG_TAG, "Passwords don't match");
            walletPasswordConfirm.setError("Passwords don't match");
            return false;
        }

        return true;
    }

    private void clearInputFields() {
        walletPassword.setText("");
        walletPasswordConfirm.setText("");
    }

    private void disableElements() {
        progressBar.setVisibility(View.VISIBLE);
        createWalletButton.setEnabled(false);
        createWalletButton.setVisibility(View.INVISIBLE);
        walletPassword.setEnabled(false);
        walletPasswordConfirm.setEnabled(false);
    }

    private void enableElements() {
        progressBar.setVisibility(View.INVISIBLE);
        createWalletButton.setVisibility(View.VISIBLE);
        createWalletButton.setEnabled(true);
        walletPassword.setEnabled(true);
        walletPasswordConfirm.setEnabled(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.welcome_fragment, container, false);
        ButterKnife.bind(this, view);
        return view;
    }
}
