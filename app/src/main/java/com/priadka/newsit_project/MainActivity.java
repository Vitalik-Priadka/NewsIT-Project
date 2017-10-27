package com.priadka.newsit_project;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.priadka.newsit_project.DTO.UserDTO;
import com.priadka.newsit_project.fragment.FullStateFragment;
import com.priadka.newsit_project.fragment.LoginFragment;
import com.priadka.newsit_project.fragment.NewsFragment;
import com.priadka.newsit_project.fragment.SettingFragment;

import java.lang.reflect.Field;

import static com.priadka.newsit_project.Constant.APP_PREFERENCES;
import static com.priadka.newsit_project.Constant.DEFAULT_AVATAR;
import static com.priadka.newsit_project.Constant.DEFAULT_THEME;

public class MainActivity extends FragmentActivity {

    private Toolbar toolbar;                public static FragmentManager manager;
    private DrawerLayout drawerLayout;      private NavigationView navigationView;
    private EditText searchField;           private LoginFragment loginFragment;
    private SharedPreferences mSettings;    private SettingFragment settingFragment;
    private static long back_pressed;       private NewsFragment newsFragment;
    private UserDTO user;                   private FullStateFragment fullStateFragment;

    private boolean isLogin, savePassword;
    private String Login, Password;
    private int currentTheme, resumeCount = 0;

    /*TODO TaskList:
        - REST API используя retrofit (парсин данных);
        + Фрагмент для статьи;
        + Динамическая подгрузка фрагментов (статей);
        + Просморт фрагмента статьи;
        - Изменение темы и языка (без статей);
        - Реализовать алгоритм поиска (желательно по частичному совпадению);
        - Реализовать алгоритм обновления (по дате добавления);
        - Комментирование статей;
        - Добавление в "Закладки"
        + Работа с кешом (сохранение темы, языка, логина, пароля);
        + Логин пользователя;
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        initMockUser();
        doPreferences(false);
        getTheme(currentTheme);
        super.onCreate(savedInstanceState);
        setContentView(Constant.LAYOUT);
        manager = getSupportFragmentManager();
        loginFragment = new LoginFragment();
        settingFragment = new SettingFragment();
        newsFragment = new NewsFragment();
        fullStateFragment = new FullStateFragment();
        FragmentDo(newsFragment);
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (resumeCount == 0) {
            initToolbar();
            initNavigationView();
            KeyboardAction();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        doPreferences(true);
    }
    @Override
    protected void onResume() {
        super.onResume();
        resumeCount++;
        doPreferences(false);
        if (isLogin) loginUser(Password);
        initNavigationOnLogin();
    }

    private void initMockUser() {
        user = new UserDTO("Vitalik","vitalik.pryadka@gmail.com", "12345", 3 ,"105,108");
    }
    public Resources.Theme getTheme(int currentTheme) {
        Resources.Theme theme = super.getTheme();
            if (currentTheme < 3 && currentTheme >= 0){
                switch (currentTheme){
                    case 0:{
                        theme.applyStyle(Constant.THEME_1, true);break;
                    }
                    case 1:{
                        theme.applyStyle(Constant.THEME_2, true);break;
                    }
                    case 2:{
                        theme.applyStyle(Constant.THEME_3, true);break;
                    }
                }
            }
        return theme;
    }
    public void doPreferences(boolean save){
        SharedPreferences.Editor editor = mSettings.edit();
        if (save) {
            editor.putBoolean("Value_savePassword", savePassword);
            editor.putBoolean("Value_isLogin", isLogin);
            editor.putInt("Value_Theme", currentTheme);
            editor.putInt("Value_Avatar", user.getUser_image());
            editor.putString("Value_Login", Login);
            editor.putString("Value_Password", Password);
            editor.apply();
        }
        else {
            savePassword = mSettings.getBoolean("Value_savePassword", false);
            isLogin = mSettings.getBoolean("Value_isLogin", false);
            currentTheme = mSettings.getInt("Value_Theme",DEFAULT_THEME);
            user.setUser_image(mSettings.getInt("Value_Avatar",DEFAULT_AVATAR));
            Login = mSettings.getString("Value_Login", "");
            Password = mSettings.getString("Value_Password", "");
        }
    }
    public void loginUser(String password){
        if (Login.equals(user.getUser_login()) && password.equals(user.getUser_password())){
            if (resumeCount <= 1){
                Toast.makeText(MainActivity.this, getString(R.string.log_success)+ " " + user.getUser_login() + "!", Toast.LENGTH_SHORT).show();
            }
            isLogin = true;
            doPreferences(true);
            FragmentDo(newsFragment);
        }
        else {
            isLogin = false;
            Toast.makeText(MainActivity.this, getString(R.string.log_error_password), Toast.LENGTH_SHORT).show();
        }
        initNavigationOnLogin();
    }

    public void initToolbar(){
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        searchField = (EditText) findViewById(R.id.search_text);
        toolbar.setTitle(null);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener(){
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.search:{
                        hideKeyboard();
                        search(searchField.getText().toString());
                        break;
                    }
                    case R.id.reload:{
                        hideKeyboard();
                        reloadPage();
                        break;
                    }
                }
                return false;
            }
        });

        toolbar.inflateMenu(R.menu.menu);
    }
    public void initNavigationView() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout , toolbar, R.string.view_navigation_open, R.string.view_navigation_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView = (NavigationView) findViewById(R.id.navigation);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                drawerLayout.closeDrawers(); hideKeyboard(); doPreferences(true);
                switch (menuItem.getItemId()){
                    case R.id.actionLogInItem:{
                        FragmentDo(loginFragment);
                        break;
                    }
                    case R.id.actionLogOutItem:{
                        isLogin = false;
                        initNavigationOnLogin();
                        reloadPage();
                        doPreferences(true);
                        break;
                    }
                    case R.id.actionBookmarks:{

                        break;
                    }
                    case R.id.actionNewsItem:{
                        FragmentDo(newsFragment);
                        break;
                    }
                    case R.id.actionSettingItem:{
                        FragmentDo(settingFragment);
                        break;
                    }
                    case R.id.actionExitItem: {
                        finishAffinity();
                        break;}
                }
                return true;
            }
        });
    }
    public void initNavigationOnLogin() {
        View hView =  navigationView.getHeaderView(0);
        ImageView avatarImage  = (ImageView)hView.findViewById(R.id.menu_avatar);
        TextView fieldUserName = (TextView)hView.findViewById(R.id.menu_nickname);
        TextView fieldUserMail = (TextView)hView.findViewById(R.id.menu_email);
        RelativeLayout headerImage = (RelativeLayout) hView.findViewById(R.id.navigation_header);
        if(isLogin){
            MenuItem LogOutButton = navigationView.getMenu().findItem(R.id.actionLogOutItem);   LogOutButton.setVisible(true);
            MenuItem BookmarksButton = navigationView.getMenu().findItem(R.id.actionBookmarks); BookmarksButton.setVisible(true);
            MenuItem LogInButton = navigationView.getMenu().findItem(R.id.actionLogInItem);     LogInButton.setVisible(false);
            this.invalidateOptionsMenu();

            // Чтение профиля и установка
            fieldUserName.setText(user.getUser_login());
            fieldUserMail.setText(user.getUser_email());
            String currentAvatarS = "avatar_" + user.getUser_image();
            avatarImage.setImageResource(getResId(currentAvatarS, R.drawable.class));
            headerImage.setVisibility(View.VISIBLE);
        }
        else {
            MenuItem LogOutButton = navigationView.getMenu().findItem(R.id.actionLogOutItem);   LogOutButton.setVisible(false);
            MenuItem BookmarksButton = navigationView.getMenu().findItem(R.id.actionBookmarks); BookmarksButton.setVisible(false);
            MenuItem LogInButton = navigationView.getMenu().findItem(R.id.actionLogInItem);     LogInButton.setVisible(true);
            this.invalidateOptionsMenu();
            headerImage.setVisibility(View.INVISIBLE);
        }
    }
    public void setImageAvatarNext(View view){
        ImageView avatarImage  = (ImageView) findViewById(R.id.menu_avatar);
        if(user.getUser_image() < 0 || user.getUser_image() >= 12){
            user.setUser_image(DEFAULT_AVATAR);
        }
        else user.setUser_image(user.getUser_image() + 1);
        String currentAvatarS = "avatar_" + user.getUser_image();
        avatarImage.setImageResource(getResId(currentAvatarS, R.drawable.class));
    }
    public static int getResId(String resName, Class<?> c) {
        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void reloadPage(){
        /* ImageView drawable.icon =(ImageView) findViewById(R.id.reload);;
        RotateAnimation rotate = new RotateAnimation(0, 180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(5000);
        rotate.setInterpolator(new LinearInterpolator());
        drawable.icon.startAnimation(rotate); */
        Toast.makeText(MainActivity.this, R.string.reload_toast, Toast.LENGTH_SHORT).show();
        resumeCount++;
        if (isLogin) loginUser(Password);
        initNavigationOnLogin();
    }
    public void search(String request){
        if(request.length() > 0) {
            Toast.makeText(MainActivity.this, R.string.search_toast, Toast.LENGTH_SHORT).show();
            searchField.setText(null);
        }
    }
    public void FragmentDo(Fragment thisFragment){
        FragmentTransaction transaction;
        if (thisFragment != null && !thisFragment.isVisible()) {
            transaction = manager.beginTransaction();
            transaction.replace(R.id.container, thisFragment);
            transaction.addToBackStack(null);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.commit();
        }
    }
    public void submitToEmail(View view) {
        // Отправка на email
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto: it.news@dev.com"));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.setting_info_email_header));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.setting_info_email_text));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            CharSequence text = getString(R.string.error_text);
            Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    public void onBackPressed() {
        if (back_pressed + 1200 > System.currentTimeMillis())
            finishAffinity();
        else {
            FragmentDo(newsFragment);
        }
        back_pressed = System.currentTimeMillis();
    }
    public void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    private  void KeyboardAction() {
        searchField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    hideKeyboard();
                    search(searchField.getText().toString());
                    return true;
                }
                return false;
            }
        });
    }

    // Getter and Setter
    public int getCurrentTheme(){return currentTheme;}
    public void setCurrentTheme(int value){currentTheme = value;}
    public boolean getIsLogin(){return isLogin;}
    public void setIsLogin(boolean value){isLogin = value;}
    public String getLogin(){return Login;}
    public void setLogin(String value){Login = value;}
    public String getPassword(){return Password;}
    public void setPassword(String value){Password = value;}
    public boolean getSavePassword(){return savePassword;}
    public void setSavePassword(boolean values){savePassword = values;}
}
