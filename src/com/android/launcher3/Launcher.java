/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.Manifest;
import android.animation.*;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.*;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Process;
import android.os.*;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.*;
import android.view.View.OnLongClickListener;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.android.launcher3.DropTarget.DragObject;
import com.android.launcher3.LauncherSettings.Favorites;
import com.android.launcher3.Workspace.ItemOperator;
import com.android.launcher3.accessibility.LauncherAccessibilityDelegate;
import com.android.launcher3.alarm.AlarmReceiver;
import com.android.launcher3.alarm.AlarmsService;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.allapps.AllAppsTransitionController;
import com.android.launcher3.anim.AnimationLayerSet;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.LauncherAppsCompatVO;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.dragndrop.*;
import com.android.launcher3.dynamicui.ExtractedColors;
import com.android.launcher3.dynamicui.WallpaperColorInfo;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.keyboard.CustomActionsPopup;
import com.android.launcher3.keyboard.ViewGroupFocusHelper;
import com.android.launcher3.logger.FirebaseLogger;
import com.android.launcher3.logger.LogEntryProfileEdited;
import com.android.launcher3.logger.LogEntryProfileTriggered;
import com.android.launcher3.logger.LogEntryUnlocks;
import com.android.launcher3.logging.FileLog;
import com.android.launcher3.logging.UserEventDispatcher;
import com.android.launcher3.model.ModelWriter;
import com.android.launcher3.model.PackageItemInfo;
import com.android.launcher3.model.WidgetItem;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.pageindicators.PageIndicator;
import com.android.launcher3.pageindicators.PageIndicatorCaretLandscape;
import com.android.launcher3.popup.PopupContainerWithArrow;
import com.android.launcher3.popup.PopupDataProvider;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.userevent.nano.LauncherLogProto.Action;
import com.android.launcher3.userevent.nano.LauncherLogProto.ContainerType;
import com.android.launcher3.userevent.nano.LauncherLogProto.ControlType;
import com.android.launcher3.util.*;
import com.android.launcher3.widget.*;
import com.google.android.apps.nexuslauncher.ProfilesActivity;
import com.google.android.apps.nexuslauncher.SettingsActivity;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.Executor;

import static android.app.WallpaperManager.FLAG_SYSTEM;
import static com.android.launcher3.R.color.primary_white;
import static com.android.launcher3.util.RunnableWithId.RUNNABLE_ID_BIND_APPS;
import static com.android.launcher3.util.RunnableWithId.RUNNABLE_ID_BIND_WIDGETS;

/**
 * Default launcher application.
 */
public class Launcher extends BaseActivity
        implements LauncherExterns, View.OnClickListener, OnLongClickListener,
        LauncherModel.Callbacks, View.OnTouchListener, LauncherProviderChangeListener,
        AccessibilityManager.AccessibilityStateChangeListener,
        WallpaperColorInfo.OnThemeChangeListener {
    public static final String TAG = "Launcher";
    static final boolean LOGD = false;

    private FirebaseLogger firebaseLogger;

    static final boolean DEBUG_WIDGETS = false;
    static final boolean DEBUG_STRICT_MODE = false;
    static final boolean DEBUG_RESUME_TIME = false;

    private static final int REQUEST_CREATE_SHORTCUT = 1;
    private static final int REQUEST_CREATE_APPWIDGET = 5;

    private static final int REQUEST_PICK_APPWIDGET = 9;
    public static final int REQUEST_PICK_WALLPAPER = 10;

    private static final int REQUEST_BIND_APPWIDGET = 11;
    private static final int REQUEST_BIND_PENDING_APPWIDGET = 12;
    private static final int REQUEST_RECONFIGURE_APPWIDGET = 13;

    private static final int REQUEST_PERMISSION_CALL_PHONE = 14;

    private static final int REQUEST_PERMISSION_EXTERNAL_STORAGE = 15;

    private static final float BOUNCE_ANIMATION_TENSION = 1.3f;

    public static final String WALLPAPER_INFO = "wallpaper_info";

    public static boolean isMinimalDesignON;

    /**
     * IntentStarter uses request codes starting with this. This must be greater than all activity
     * request codes used internally.
     */
    protected static final int REQUEST_LAST = 100;

    private static final int SOFT_INPUT_MODE_DEFAULT =
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
    private static final int SOFT_INPUT_MODE_ALL_APPS =
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

    // The Intent extra that defines whether to ignore the launch animation
    static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION =
            "com.android.launcher3.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";

    // Type: int
    private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    // Type: int
    private static final String RUNTIME_STATE = "launcher.state";
    // Type: PendingRequestArgs
    private static final String RUNTIME_STATE_PENDING_REQUEST_ARGS = "launcher.request_args";
    // Type: ActivityResultInfo
    private static final String RUNTIME_STATE_PENDING_ACTIVITY_RESULT = "launcher.activity_result";

    static final String APPS_VIEW_SHOWN = "launcher.apps_view_shown";

    /** The different states that Launcher can be in. */
    enum State {
        NONE, WORKSPACE, WORKSPACE_SPRING_LOADED, APPS, APPS_SPRING_LOADED,
        WIDGETS, WIDGETS_SPRING_LOADED
    }

    @Thunk
    State mState = State.WORKSPACE;
    @Thunk
    LauncherStateTransitionAnimation mStateTransitionAnimation;

    private boolean mIsSafeModeEnabled;

    public static final int EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT = 500;
    private static final int ON_ACTIVITY_RESULT_ANIMATION_DELAY = 500;

    // How long to wait before the new-shortcut animation automatically pans the workspace
    private static final int NEW_APPS_PAGE_MOVE_DELAY = 500;
    private static final int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 5;
    @Thunk
    static final int NEW_APPS_ANIMATION_DELAY = 500;

    private final ExtractedColors mExtractedColors = new ExtractedColors();

    @Thunk
    Workspace mWorkspace;
    private View mLauncherView;
    @Thunk
    DragLayer mDragLayer;
    private DragController mDragController;

    public View mWeightWatcher;

    private AppWidgetManagerCompat mAppWidgetManager;
    private LauncherAppWidgetHost mAppWidgetHost;

    private final int[] mTmpAddItemCellCoordinates = new int[2];

    @Thunk
    Hotseat mHotseat;
    private ViewGroup mOverviewPanel;

    private View mAllAppsButton;
    private View mWidgetsButton;

    private DropTargetBar mDropTargetBar;

    // Main container view for the all apps screen.
    @Thunk
    AllAppsContainerView mAppsView;
    AllAppsTransitionController mAllAppsController;

    // Main container view and the model for the widget tray screen.
    @Thunk
    WidgetsContainerView mWidgetsView;

    // We need to store the orientation Launcher was created with, due to a bug (b/64916689)
    // that results in widgets being inflated in the wrong orientation.
    private int mOrientation;

    // We set the state in both onCreate and then onNewIntent in some cases, which causes both
    // scroll issues (because the workspace may not have been measured yet) and extra work.
    // Instead, just save the state that we need to restore Launcher to, and commit it in onResume.
    private State mOnResumeState = State.NONE;

    private SpannableStringBuilder mDefaultKeySsb = null;

    @Thunk
    boolean mWorkspaceLoading = true;

    private boolean mPaused = true;
    private boolean mOnResumeNeedsLoad;

    private final ArrayList<Runnable> mBindOnResumeCallbacks = new ArrayList<>();
    private final ArrayList<Runnable> mOnResumeCallbacks = new ArrayList<>();
    private ViewOnDrawExecutor mPendingExecutor;

    private LauncherModel mModel;
    private ModelWriter mModelWriter;
    private IconCache mIconCache;
    private LauncherAccessibilityDelegate mAccessibilityDelegate;
    private final Handler mHandler = new Handler();
    private boolean mHasFocus = false;

    private ObjectAnimator mScrimAnimator;
    private boolean mShouldFadeInScrim;

    private PopupDataProvider mPopupDataProvider;

    // Determines how long to wait after a rotation before restoring the screen orientation to
    // match the sensor state.
    private static final int RESTORE_SCREEN_ORIENTATION_DELAY = 500;

    private final ArrayList<Integer> mSynchronouslyBoundPages = new ArrayList<>();

    // We only want to get the SharedPreferences once since it does an FS stat each time we get
    // it from the context.
    public static SharedPreferences mSharedPrefs;

    private boolean mMoveToDefaultScreenFromNewIntent;

    // This is set to the view that launched the activity that navigated the user away from
    // launcher. Since there is no callback for when the activity has finished launching, enable
    // the press state and keep this reference to reset the press state when we return to launcher.
    private BubbleTextView mWaitingForResume;

    protected static final HashMap<String, CustomAppWidget> sCustomAppWidgets =
            new HashMap<>();

    static {
        if (TestingUtils.ENABLE_CUSTOM_WIDGET_TEST) {
            TestingUtils.addDummyWidget(sCustomAppWidgets);
        }
    }

    // Exiting spring loaded mode happens with a delay. This runnable object triggers the
    // state transition. If another state transition happened during this delay,
    // simply unregister this runnable.
    private Runnable mExitSpringLoadedModeRunnable;

    @Thunk
    final Runnable mBuildLayersRunnable = new Runnable() {
        public void run() {
            if (mWorkspace != null) {
                mWorkspace.buildPageHardwareLayers();
            }
        }
    };

    // Activity result which needs to be processed after workspace has loaded.
    private ActivityResultInfo mPendingActivityResult;
    /**
     * Holds extra information required to handle a result from an external call, like
     * {@link #startActivityForResult(Intent, int)} or {@link #requestPermissions(String[], int)}
     */
    private PendingRequestArgs mPendingRequestArgs;

    private float mLastDispatchTouchEventX = 0.0f;

    public ViewGroupFocusHelper mFocusHandler;
    private boolean mRotationEnabled = false;

    @Thunk
    void setOrientation() {
        if (mRotationEnabled) {
            unlockScreenOrientation(true);
        } else {
            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }
    }

    private RotationPrefChangeHandler mRotationPrefChangeHandler;
    private SSIDPrefChangeHandler mSSIDPrefChangeHandler;
    private SchedulePrefChangeHandler mSchedulePrefChangeHandler;
    private MinimalDesignPrefChangeHandler mMinimalDesignPrefChangeHandler;
    private WallpaperButtonClickedHandler wallpaperButtonClickedHandler;
    private ProfileChangeHandler profileChangeHandler;
    private LogProfileEditedHandler mLogProfileEditedHandler;

    private ListView launcherListView;
    private ListView allAppsListView;
    private PackageManager packageManager;
    private ArrayList<String> allPackageNames;
    private ArrayList<String> homescreenPackageNames;
    private ArrayAdapter<String> adapterAll;
    private ArrayAdapter<String> adapterHomescreen;
    public ViewFlipper viewFlipper;
    private Button settingsButton;
    private Button mProfilesButton;
    private Button allAppsButton;
    private static Set<String> set = new HashSet<String>();

    public static ArrayList<String> availableProfiles;

    public final static String APPS_ON_HOMESCREEN = "apps_on_homescreen";
    private final static String ALL_APPS = "all_apps";

    public final static String CURRENT_PROFILE_PREF = "current_profile";
    public final static String MANUAL_PROFILE_PREF = "manual_profile";

    public static ArrayList<String> newAddedProfiles;

    private static boolean isRecreatedForThemeChange = false;

    private static ArrayList<String> usedApps = new ArrayList<>();

    public static ArrayList<String> usedShortcuts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG_STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        if (LauncherAppState.PROFILE_STARTUP) {
            Trace.beginSection("Launcher-onCreate");
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.preOnCreate();
        }

        WallpaperColorInfo wallpaperColorInfo = WallpaperColorInfo.getInstance(this);
        wallpaperColorInfo.setOnThemeChangeListener(this);
        overrideTheme(wallpaperColorInfo.isDark(), wallpaperColorInfo.supportsDarkText(), wallpaperColorInfo.isTransparent());

        super.onCreate(savedInstanceState);

        hasExternalStoragePermission(this);

        LauncherAppState app = LauncherAppState.getInstance(this);

        // Load configuration-specific DeviceProfile
        mDeviceProfile = app.getInvariantDeviceProfile().getDeviceProfile(this);
        if (isInMultiWindowModeCompat()) {
            Display display = getWindowManager().getDefaultDisplay();
            Point mwSize = new Point();
            display.getSize(mwSize);
            mDeviceProfile = mDeviceProfile.getMultiWindowProfile(this, mwSize);
        }

        mOrientation = getResources().getConfiguration().orientation;
        mSharedPrefs = Utilities.getPrefs(this);

        //Firebase Logging
        firebaseLogger = FirebaseLogger.getInstance();

        //creating a user ID that is added to each log message in the Firebase database
        String userID = mSharedPrefs.getString("userID_firebase", null);
        if (userID == null) {
            userID = UUID.randomUUID().toString().substring(0, 7);
            Launcher.mSharedPrefs.edit().putString("userID_firebase", userID).apply();
            firebaseLogger.setUserID(userID);
        }
        firebaseLogger.setUserID(userID);

        Set<String> set1 = Launcher.mSharedPrefs.getStringSet(ProfilesActivity.PROFILES_MANAGED, null);
        if (set1 == null) {
            availableProfiles = new ArrayList<>();
            availableProfiles.add("home");
            availableProfiles.add("work");
            availableProfiles.add("default");
            availableProfiles.add("disconnected");
            Set<String> setNewAddedProfiles = Launcher.mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
            if (setNewAddedProfiles != null) {
                newAddedProfiles = new ArrayList<>(setNewAddedProfiles);
                for (String newAddedProfile : newAddedProfiles) {
                    availableProfiles.add(newAddedProfile.substring(1));
                }
            }
            Set<String> set2 = new HashSet<>(availableProfiles);
            Launcher.mSharedPrefs.edit().putStringSet(ProfilesActivity.PROFILES_MANAGED, set2).apply();
        } else {
            availableProfiles = new ArrayList<>(set1);
        }

        if (mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null) == null) {
            newAddedProfiles = new ArrayList<>();
        } else {
            Set<String> set = Launcher.mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
            newAddedProfiles = new ArrayList<>(set);
        }

        mIsSafeModeEnabled = getPackageManager().isSafeMode();
        mModel = app.setLauncher(this);
        mModelWriter = mModel.getWriter(mDeviceProfile.isVerticalBarLayout());
        mIconCache = app.getIconCache();
        mAccessibilityDelegate = new LauncherAccessibilityDelegate(this);

        mDragController = new DragController(this);
        mAllAppsController = new AllAppsTransitionController(this);
        mStateTransitionAnimation = new LauncherStateTransitionAnimation(this, mAllAppsController);

        mAppWidgetManager = AppWidgetManagerCompat.getInstance(this);

        mAppWidgetHost = new LauncherAppWidgetHost(this);
        if (Utilities.ATLEAST_MARSHMALLOW) {
            mAppWidgetHost.addProviderChangeListener(this);
        }
        mAppWidgetHost.startListening();

        // If we are getting an onCreate, we can actually preempt onResume and unset mPaused here,
        // this also ensures that any synchronous binding below doesn't re-trigger another
        // LauncherModel load.
        mPaused = false;

        mLauncherView = LayoutInflater.from(this).inflate(R.layout.launcher, null);

        setupViews();
        mDeviceProfile.layout(this, false /* notifyListeners */);
        loadExtractedColorsAndColorItems();

        mPopupDataProvider = new PopupDataProvider(this);

        ((AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE))
                .addAccessibilityStateChangeListener(this);

        lockAllApps();

        restoreState(savedInstanceState);

        if (LauncherAppState.PROFILE_STARTUP) {
            Trace.endSection();
        }

        // We only load the page synchronously if the user rotates (or triggers a
        // configuration change) while launcher is in the foreground
        int currentScreen = PagedView.INVALID_RESTORE_PAGE;
        if (savedInstanceState != null) {
            currentScreen = savedInstanceState.getInt(RUNTIME_STATE_CURRENT_SCREEN, currentScreen);
        }
        if (!mModel.startLoader(currentScreen)) {
            // If we are not binding synchronously, show a fade in animation when
            // the first page bind completes.
            mDragLayer.setAlpha(0);
        } else {
            // Pages bound synchronously.
            mWorkspace.setCurrentPage(currentScreen);

            setWorkspaceLoading(true);
        }

        //checkLocationPermission(this);
        checkFineLocationPermission(this);
        hasWritePermission(this, true);

        // For handling default keys
        mDefaultKeySsb = new SpannableStringBuilder();
        Selection.setSelection(mDefaultKeySsb, 0);

        mRotationEnabled = getResources().getBoolean(R.bool.allow_rotation);
        // In case we are on a device with locked rotation, we should look at preferences to check
        // if the user has specifically allowed rotation.
        if (!mRotationEnabled) {
            mRotationEnabled = Utilities.isAllowRotationPrefEnabled(getApplicationContext());
            mRotationPrefChangeHandler = new RotationPrefChangeHandler();
            mSharedPrefs.registerOnSharedPreferenceChangeListener(mRotationPrefChangeHandler);
        }

        if (PinItemDragListener.handleDragRequest(this, getIntent())) {
            // Temporarily enable the rotation
            mRotationEnabled = true;
        }

        mSchedulePrefChangeHandler = new SchedulePrefChangeHandler();
        mSharedPrefs.registerOnSharedPreferenceChangeListener(mSchedulePrefChangeHandler);

        mMinimalDesignPrefChangeHandler = new MinimalDesignPrefChangeHandler();
        mSharedPrefs.registerOnSharedPreferenceChangeListener(mMinimalDesignPrefChangeHandler);

        wallpaperButtonClickedHandler = new WallpaperButtonClickedHandler();
        mSharedPrefs.registerOnSharedPreferenceChangeListener(wallpaperButtonClickedHandler);

        profileChangeHandler = new ProfileChangeHandler();
        mSharedPrefs.registerOnSharedPreferenceChangeListener(profileChangeHandler);

        mLogProfileEditedHandler = new LogProfileEditedHandler();
        mSharedPrefs.registerOnSharedPreferenceChangeListener(mLogProfileEditedHandler);

        if(hasExternalStoragePermission(this)){
            saveCurrentWallpaper();
            saveWallpaperInfo();
        }

        saveCurrentRingtones();

        // On large interfaces, or on devices that a user has specifically enabled screen rotation,
        // we want the screen to auto-rotate based on the current orientation
        setOrientation();

        setContentView(mLauncherView);

        // Listen for broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT); // When the device wakes up + keyguard is gone
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mReceiver, filter);
        mShouldFadeInScrim = true;

        filter = new IntentFilter();
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.net.wifi.supplicant.CONNECTION_CHANGE");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        filter.setPriority(100);
        registerReceiver(mWiFiReceiver, filter);
        /*
        filter = new IntentFilter();
        filter.addAction(AlarmsService.ACTION_COMPLETE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mAlarmReceiver, filter);
        AlarmsService.launchAlarmsService(this);
         */


        mSSIDPrefChangeHandler = new SSIDPrefChangeHandler();
        mSharedPrefs.registerOnSharedPreferenceChangeListener(mSSIDPrefChangeHandler);

        getSystemUiController().updateUiState(SystemUiController.UI_STATE_BASE_WINDOW,
                Themes.getAttrBoolean(this, R.attr.isWorkspaceDarkText));

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onCreate(savedInstanceState);
        }


        viewFlipper = (ViewFlipper) findViewById(R.id.launcher_view_flipper);

        settingsButton = (Button) findViewById(R.id.minimal_settings_button);
        mProfilesButton = (Button) findViewById(R.id.minimal_profile_button);
        allAppsButton = (Button) findViewById(R.id.minimal_apps_button);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        mProfilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ProfilesActivity.class);
                //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        allAppsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                allAppsListView.setVisibility(View.VISIBLE);
                launcherListView.setVisibility(View.INVISIBLE);
            }
        });

        launcherListView = findViewById(R.id.launcher_list_view);
        launcherListView.setVerticalScrollBarEnabled(false);
        launcherListView.setDivider(null);

        allAppsListView = findViewById(R.id.all_app_list_view);
        allAppsListView.setVerticalScrollBarEnabled(false);
        allAppsListView.setDivider(null);
        allAppsListView.setBackgroundColor(getColor(primary_white));

        // Get a list of all the apps installed
        packageManager = getPackageManager();
        adapterAll = new ArrayAdapter<String>(
                this, R.layout.launcher_list_item, new ArrayList<String>());
        adapterHomescreen = new ArrayAdapter<String>(
                this, R.layout.launcher_list_item, new ArrayList<String>());
        allPackageNames = new ArrayList<>();
        homescreenPackageNames = new ArrayList<>();

        // Tap on an item in the list to launch the app
        launcherListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    startActivity(packageManager.getLaunchIntentForPackage(homescreenPackageNames.get(position)));
                    String currentProfile = mSharedPrefs.getString(CURRENT_PROFILE_PREF, null);
                    usedShortcuts.add(adapterHomescreen.getItem(position) + "_" + currentProfile);
                } catch (Exception e) {
                    fetchHomescreenAppList();
                }
            }
        });

        allAppsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    startActivity(packageManager.getLaunchIntentForPackage(allPackageNames.get(position)));
                    String currentProfile = mSharedPrefs.getString(CURRENT_PROFILE_PREF, null);
                    usedApps.add(adapterAll.getItem(position) + "_" + currentProfile);
                } catch (Exception e) {
                    fetchAllAppList();
                }
            }
        });

        // Long press on an item in the list to open the app settings
        launcherListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    // Attempt to launch the app with the package name
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + homescreenPackageNames.get(position)));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    fetchHomescreenAppList();
                }
                return false;
            }
        });

        allAppsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    // Attempt to launch the app with the package name
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + allPackageNames.get(position)));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    fetchAllAppList();
                }
                return false;
            }
        });

        //set.clear();

        fetchAllAppList();
        fetchHomescreenAppList();

        String currentProfile = mSharedPrefs.getString(CURRENT_PROFILE_PREF, null);
        if (currentProfile != null) {
            if (currentProfile.equals("home") || currentProfile.equals("work") || currentProfile.equals("default") || currentProfile.equals("disconnected")) {
                isMinimalDesignON = mSharedPrefs.getBoolean(currentProfile + ProfilesActivity.MINIMAL_DESIGN_PREF, false);
                switchToMinimalLayout(isMinimalDesignON);
            } else {
                for (String sub : newAddedProfiles) {
                    if (sub.substring(1).equals(currentProfile)) {
                        isMinimalDesignON = mSharedPrefs.getBoolean(sub.charAt(0) + ProfilesActivity.MINIMAL_DESIGN_PREF, false);
                        switchToMinimalLayout(isMinimalDesignON);
                    }
                }
            }
        }
    }

    public void switchToMinimalLayout(boolean value) {
        if (value) {
            viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.launcher_minimalist_layout)));
        } else {
            viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.drag_layer)));
        }
    }

    public static void getAppItemsFromLoaderResults(List<ItemInfo> workspaceItems) {
        if (!workspaceItems.isEmpty()) {
        }
    }

    private void fetchHomescreenAppList() {
        // Start from a clean adapter when refreshing the list
        if (adapterHomescreen != null) {
            adapterHomescreen.clear();
        }
        if (homescreenPackageNames != null) {
            homescreenPackageNames.clear();
        }
        if (packageManager != null) {
            // Query the package manager for all apps
            List<ResolveInfo> activities = packageManager.queryIntentActivities(
                    new Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER), 0);

            // Sort the applications by alphabetical order and add them to the list
            Collections.sort(activities, new ResolveInfo.DisplayNameComparator(packageManager));
            Set<String> appsOnHomescreen = mSharedPrefs.getStringSet(APPS_ON_HOMESCREEN, null);
            String currentProfile = mSharedPrefs.getString(CURRENT_PROFILE_PREF, null);
            if (appsOnHomescreen != null && currentProfile != null) {
                if(currentProfile.equals("home")||currentProfile.equals("disconnected")||currentProfile.equals("default")||currentProfile.equals("work")){
                    // do nothing
                } else {
                    for(String newAddedProfile : newAddedProfiles){
                        if(newAddedProfile.substring(1).equals(currentProfile)){
                            currentProfile = newAddedProfile.charAt(0)+"";
                        }
                    }
                }
                for (String profileApps : appsOnHomescreen) {
                    String profileName = profileApps.split("_")[0];
                    if (profileName.equals(currentProfile)) {
                        if (profileApps.split("_").length > 1) {
                            List<String> listProfileApps = Arrays.asList(profileApps.split("_")[1].split(","));
                            for (String app : listProfileApps) {
                                for (ResolveInfo resolver : activities) {
                                    // Exclude  this launcher from the list of apps shown
                                    String appName = (String) resolver.loadLabel(packageManager);
                                    if (appName.equals("Focus Launcher")) continue;
                                    if (appName.equals(app)) {
                                        adapterHomescreen.add(appName);
                                        homescreenPackageNames.add(resolver.activityInfo.packageName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            launcherListView.setAdapter(adapterHomescreen);
        }
    }

    private void fetchAllAppList() {
        // Start from a clean adapter when refreshing the list
        if (adapterAll != null) {
            adapterAll.clear();
        }
        if (allPackageNames != null) {
            allPackageNames.clear();
        }
        if (packageManager != null) {
            // Query the package manager for all apps
            List<ResolveInfo> activities = packageManager.queryIntentActivities(
                    new Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER), 0);

            // Sort the applications by alphabetical order and add them to the list
            Collections.sort(activities, new ResolveInfo.DisplayNameComparator(packageManager));
            for (ResolveInfo resolver : activities) {

                // Exclude the settings app and this launcher from the list of apps shown
                String appName = (String) resolver.loadLabel(packageManager);
                if (appName.equals("Focus Launcher")) continue;
                adapterAll.add(appName);
                allPackageNames.add(resolver.activityInfo.packageName);
            }
            allAppsListView.setAdapter(adapterAll);
        }
    }

    @Override
    public void onThemeChanged() {
        isRecreatedForThemeChange = true;
        recreate();
    }

    protected void overrideTheme(boolean isDark, boolean supportsDarkText, boolean isTransparent) {
        mColorThemeLight = Color.parseColor(isDark ? "#f8f8f8" : "#4285f4");
        mColorThemeDark = Color.parseColor(isDark && !supportsDarkText ? "#f8f8f8" : "#4f4f4f");
        if (isDark) {
            setTheme(R.style.LauncherThemeDark);
        } else if (supportsDarkText) {
            setTheme(R.style.LauncherThemeDarkText);
        } else if (isTransparent) {
            setTheme(R.style.LauncherThemeTransparent);
        }
    }

    @Override
    public <T extends View> T findViewById(int id) {
        return mLauncherView.findViewById(id);
    }

    @Override
    public void onExtractedColorsChanged() {
        loadExtractedColorsAndColorItems();
        mExtractedColors.notifyChange();
    }

    public ExtractedColors getExtractedColors() {
        return mExtractedColors;
    }

    @Override
    public void onAppWidgetHostReset() {
        if (mAppWidgetHost != null) {
            mAppWidgetHost.startListening();
        }
    }

    private void loadExtractedColorsAndColorItems() {
        mExtractedColors.load(this);
        mHotseat.updateColor(mExtractedColors, !mPaused);
        mWorkspace.getPageIndicator().updateColor(mExtractedColors);
    }

    private LauncherCallbacks mLauncherCallbacks;

    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onPostCreate(savedInstanceState);
        }
    }

    public void onInsetsChanged(Rect insets) {
        mDeviceProfile.updateInsets(insets);
        mDeviceProfile.layout(this, true /* notifyListeners */);
    }

    /**
     * Call this after onCreate to set or clear overlay.
     */
    public void setLauncherOverlay(LauncherOverlay overlay) {
        if (overlay != null) {
            overlay.setOverlayCallbacks(new LauncherOverlayCallbacksImpl());
        }
        mWorkspace.setLauncherOverlay(overlay);
    }

    public boolean setLauncherCallbacks(LauncherCallbacks callbacks) {
        mLauncherCallbacks = callbacks;
        return true;
    }

    @Override
    public void onLauncherProviderChanged() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onLauncherProviderChange();
        }
    }

    /** To be overridden by subclasses to hint to Launcher that we have custom content */
    protected boolean hasCustomContentToLeft() {
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.hasCustomContentToLeft();
        }
        return false;
    }

    /**
     * To be overridden by subclasses to populate the custom content container and call
     * {@link #addToCustomContentPage}. This will only be invoked if
     * {@link #hasCustomContentToLeft()} is {@code true}.
     */
    protected void populateCustomContentContainer() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.populateCustomContentContainer();
        }
    }

    /**
     * Invoked by subclasses to signal a change to the {@link #addToCustomContentPage} value to
     * ensure the custom content page is added or removed if necessary.
     */
    protected void invalidateHasCustomContentToLeft() {
        if (mWorkspace == null || mWorkspace.getScreenOrder().isEmpty()) {
            // Not bound yet, wait for bindScreens to be called.
            return;
        }

        if (!mWorkspace.hasCustomContent() && hasCustomContentToLeft()) {
            // Create the custom content page and call the subclass to populate it.
            mWorkspace.createCustomContentContainer();
            populateCustomContentContainer();
        } else if (mWorkspace.hasCustomContent() && !hasCustomContentToLeft()) {
            mWorkspace.removeCustomContentPage();
        }
    }

    public boolean isDraggingEnabled() {
        // We prevent dragging when we are loading the workspace as it is possible to pick up a view
        // that is subsequently removed from the workspace in startBinding().
        return !isWorkspaceLoading();
    }

    public int getViewIdForItem(ItemInfo info) {
        // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
        // This cast is safe as long as the id < 0x00FFFFFF
        // Since we jail all the dynamically generated views, there should be no clashes
        // with any other views.
        return (int) info.id;
    }

    public PopupDataProvider getPopupDataProvider() {
        return mPopupDataProvider;
    }

    /**
     * Returns whether we should delay spring loaded mode -- for shortcuts and widgets that have
     * a configuration step, this allows the proper animations to run after other transitions.
     */
    private long completeAdd(
            int requestCode, Intent intent, int appWidgetId, PendingRequestArgs info) {
        long screenId = info.screenId;
        if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            // When the screen id represents an actual screen (as opposed to a rank) we make sure
            // that the drop page actually exists.
            screenId = ensurePendingDropLayoutExists(info.screenId);
        }

        switch (requestCode) {
            case REQUEST_CREATE_SHORTCUT:
                completeAddShortcut(intent, info.container, screenId, info.cellX, info.cellY, info);
                break;
            case REQUEST_CREATE_APPWIDGET:
                completeAddAppWidget(appWidgetId, info, null, null);
                break;
            case REQUEST_RECONFIGURE_APPWIDGET:
                completeRestoreAppWidget(appWidgetId, LauncherAppWidgetInfo.RESTORE_COMPLETED);
                break;
            case REQUEST_BIND_PENDING_APPWIDGET: {
                int widgetId = appWidgetId;
                LauncherAppWidgetInfo widgetInfo =
                        completeRestoreAppWidget(widgetId, LauncherAppWidgetInfo.FLAG_UI_NOT_READY);
                if (widgetInfo != null) {
                    // Since the view was just bound, also launch the configure activity if needed
                    LauncherAppWidgetProviderInfo provider = mAppWidgetManager
                            .getLauncherAppWidgetInfo(widgetId);
                    if (provider != null) {
                        new WidgetAddFlowHandler(provider)
                                .startConfigActivity(this, widgetInfo, REQUEST_RECONFIGURE_APPWIDGET);
                    }
                }
                break;
            }
        }

        return screenId;
    }

    private void handleActivityResult(
            final int requestCode, final int resultCode, final Intent data) {
        if (isWorkspaceLoading()) {
            // process the result once the workspace has loaded.
            mPendingActivityResult = new ActivityResultInfo(requestCode, resultCode, data);
            return;
        }
        mPendingActivityResult = null;

        // Reset the startActivity waiting flag
        final PendingRequestArgs requestArgs = mPendingRequestArgs;
        setWaitingForResult(null);
        if (requestArgs == null) {
            return;
        }

        final int pendingAddWidgetId = requestArgs.getWidgetId();

        Runnable exitSpringLoaded = new Runnable() {
            @Override
            public void run() {
                exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED),
                        EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT, null);
            }
        };

        if (requestCode == REQUEST_BIND_APPWIDGET) {
            // This is called only if the user did not previously have permissions to bind widgets
            final int appWidgetId = data != null ?
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (resultCode == RESULT_CANCELED) {
                completeTwoStageWidgetDrop(RESULT_CANCELED, appWidgetId, requestArgs);
                mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
            } else if (resultCode == RESULT_OK) {
                addAppWidgetImpl(
                        appWidgetId, requestArgs, null,
                        requestArgs.getWidgetHandler(),
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY);
            }
            return;
        } else if (requestCode == REQUEST_PICK_WALLPAPER) {
            //if (resultCode == RESULT_OK /* && mWorkspace.isInOverviewMode()*/) {}
            onRequestWallpaperPick();
            return;
        }

        boolean isWidgetDrop = (requestCode == REQUEST_PICK_APPWIDGET ||
                requestCode == REQUEST_CREATE_APPWIDGET);

        // We have special handling for widgets
        if (isWidgetDrop) {
            final int appWidgetId;
            int widgetId = data != null ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                    : -1;
            if (widgetId < 0) {
                appWidgetId = pendingAddWidgetId;
            } else {
                appWidgetId = widgetId;
            }

            final int result;
            if (appWidgetId < 0 || resultCode == RESULT_CANCELED) {
                Log.e(TAG, "Error: appWidgetId (EXTRA_APPWIDGET_ID) was not " +
                        "returned from the widget configuration activity.");
                result = RESULT_CANCELED;
                completeTwoStageWidgetDrop(result, appWidgetId, requestArgs);
                final Runnable onComplete = new Runnable() {
                    @Override
                    public void run() {
                        exitSpringLoadedDragModeDelayed(false, 0, null);
                    }
                };

                mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
            } else {
                if (requestArgs.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    // When the screen id represents an actual screen (as opposed to a rank)
                    // we make sure that the drop page actually exists.
                    requestArgs.screenId =
                            ensurePendingDropLayoutExists(requestArgs.screenId);
                }
                final CellLayout dropLayout =
                        mWorkspace.getScreenWithId(requestArgs.screenId);

                dropLayout.setDropPending(true);
                final Runnable onComplete = new Runnable() {
                    @Override
                    public void run() {
                        completeTwoStageWidgetDrop(resultCode, appWidgetId, requestArgs);
                        dropLayout.setDropPending(false);
                    }
                };
                mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
            }
            return;
        }

        if (requestCode == REQUEST_RECONFIGURE_APPWIDGET
                || requestCode == REQUEST_BIND_PENDING_APPWIDGET) {
            if (resultCode == RESULT_OK) {
                // Update the widget view.
                completeAdd(requestCode, data, pendingAddWidgetId, requestArgs);
            }
            // Leave the widget in the pending state if the user canceled the configure.
            return;
        }

        if (requestCode == REQUEST_CREATE_SHORTCUT) {
            // Handle custom shortcuts created using ACTION_CREATE_SHORTCUT.
            if (resultCode == RESULT_OK && requestArgs.container != ItemInfo.NO_ID) {
                completeAdd(requestCode, data, -1, requestArgs);
                mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);

            } else if (resultCode == RESULT_CANCELED) {
                mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
            }
        }
        mDragLayer.clearAnimatedView();
    }

    private void onRequestWallpaperPick() {
        Bitmap wallpaper = extractWallpaper();
        String profile = mSharedPrefs.getString(CURRENT_PROFILE_PREF, null);
        saveImageToAppPrivateFile(wallpaper, "wallpaper_" + profile);
        // User could have free-scrolled between pages before picking a wallpaper; make sure
        // we move to the closest one now.
        mWorkspace.setCurrentPage(mWorkspace.getPageNearestToCenterOfScreen());
        showWorkspace(false);
        saveWallpaperInfo();

        if (profile != null) {
            if (profile.length() > 1) {
                LogEntryProfileEdited logEntry = new LogEntryProfileEdited(profile, "wallpaper edited", getSSIDPref(profile), getSchedulePref(profile), getRingtonePref(profile), getNotificationSoundPref(profile), getNotificationBlockedPref(profile), getMinimalDesignPref(profile), getHomeScreenAppsList(profile), getWallpaperInfo(profile), getGrayScalePref(profile));
                firebaseLogger.addLogMessage("events", "profile edited", logEntry);
            } else {
                Set set = mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
                if (set != null) {
                    newAddedProfiles = new ArrayList<String>(set);
                    for (String newAddedProfile : newAddedProfiles) {
                        if (profile.equals(newAddedProfile.substring(1))) {
                            profile = newAddedProfile.charAt(0) + "";
                            LogEntryProfileEdited logEntry = new LogEntryProfileEdited(newAddedProfile.substring(1), "wallpaper edited", getSSIDPref(profile), getSchedulePref(profile), getRingtonePref(profile), getNotificationSoundPref(profile), getNotificationBlockedPref(profile), getMinimalDesignPref(profile), getHomeScreenAppsList(profile), getWallpaperInfo(profile), getGrayScalePref(profile));
                            firebaseLogger.addLogMessage("events", "profile edited", logEntry);
                        }
                    }
                }
            }
        }
    }

    private static final String INIT_RINGTONE = "INIT_RINGTONE";
    private static final String INIT_NOTIFICATION_SOUND = "INIT_NOTIFICATION_SOUND";

    private void saveCurrentRingtones() {
        Uri currentRingtone = null;
        Uri currentNotificationSound = null;

        for (String profile : availableProfiles) {
            if (profile.equals("home") || profile.equals("work") || profile.equals("default") || profile.equals("disconnected")) {
                String key = profile + "_ringtone";
                String ringtonePref = mSharedPrefs.getString(key, INIT_RINGTONE);
                if (ringtonePref.equals(INIT_RINGTONE)) {
                    if (currentRingtone == null)
                        currentRingtone = extractRingtone(RingtoneManager.TYPE_RINGTONE);
                    if(currentRingtone!=null){
                        mSharedPrefs.edit().putString(key, currentRingtone.toString()).apply();
                    }
                }

                key = profile + "_notification_sound";
                ringtonePref = mSharedPrefs.getString(key, INIT_NOTIFICATION_SOUND);
                if (ringtonePref.equals(INIT_NOTIFICATION_SOUND)) {
                    currentNotificationSound = extractRingtone(RingtoneManager.TYPE_NOTIFICATION);
                    if (currentNotificationSound != null){
                        mSharedPrefs.edit().putString(key, currentNotificationSound.toString()).apply();
                    }
                }
            } else {
                Set set = mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
                if (set != null) {
                    newAddedProfiles = new ArrayList<String>(set);
                    if (newAddedProfiles != null) {
                        for (String sub : newAddedProfiles) {
                            if (sub.substring(1).equals(profile)) {
                                String profileID = sub.charAt(0) + "";
                                String key = profileID + "_ringtone";
                                String ringtonePref = mSharedPrefs.getString(key, INIT_RINGTONE);
                                if (ringtonePref.equals(INIT_RINGTONE)) {
                                    if (currentRingtone == null)
                                        currentRingtone = extractRingtone(RingtoneManager.TYPE_RINGTONE);
                                    mSharedPrefs.edit().putString(key, currentRingtone.toString()).apply();
                                }
                                key = profileID + "_notification_sound";
                                ringtonePref = mSharedPrefs.getString(key, INIT_NOTIFICATION_SOUND);
                                if (ringtonePref.equals(INIT_NOTIFICATION_SOUND)) {
                                    if (currentNotificationSound == null)
                                        currentNotificationSound = extractRingtone(RingtoneManager.TYPE_NOTIFICATION);
                                    mSharedPrefs.edit().putString(key, currentNotificationSound.toString()).apply();
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    private Uri extractRingtone(int type) {
        if (type != RingtoneManager.TYPE_RINGTONE && type != RingtoneManager.TYPE_NOTIFICATION)
            throw new RuntimeException("Ringtone extraction failed! Unknown type \"" + type + "\"");

        Uri defaultUri = RingtoneManager.getActualDefaultRingtoneUri(this, type);
        Ringtone defaultRingtone = RingtoneManager.getRingtone(this, defaultUri);

        String defaultRingtoneTitle = defaultRingtone.getTitle(this);

        if (defaultRingtone == null) {
            Log.d("RINGTONE_EXTRACTION", "default " + (type == RingtoneManager.TYPE_RINGTONE ? "ringtone" : "notification sound") + " \"Silent\" with uri = " + Uri.EMPTY.getPath());
            return Uri.EMPTY;
        } else if (defaultRingtoneTitle.equals("Silent") || defaultRingtoneTitle.equals("Stumm")) {
            return Uri.EMPTY;
        } else {
            if(defaultUri!=null){
                Log.d("RINGTONE_EXTRACTION", "default " + (type == RingtoneManager.TYPE_RINGTONE ? "ringtone" : "notification sound") + " \"" + defaultRingtoneTitle + "\" with uri = " + defaultUri.getPath());
            }
            return defaultUri;
        }
    }


    private void saveCurrentWallpaper() {
        Bitmap currentWallpaper = null;
        for (String profile : availableProfiles) {
            if (profile.equals("home") || profile.equals("work") || profile.equals("default") || profile.equals("disconnected")) {
                String filename = "wallpaper_" + profile;
                if (!privateFileExists(filename)) {
                    if (currentWallpaper == null) currentWallpaper = extractWallpaper();
                    saveImageToAppPrivateFile(currentWallpaper, filename);
                }
            } else {
                Set set = mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
                if (set != null) {
                    newAddedProfiles = new ArrayList<>(set);
                    if (newAddedProfiles != null) {
                        for (String sub : newAddedProfiles) {
                            if (sub.substring(1).equals(profile)) {
                                String profileID = sub.charAt(0) + "";
                                String filename = "wallpaper_" + profileID;
                                if (!privateFileExists(filename)) {
                                    if (currentWallpaper == null)
                                        currentWallpaper = extractWallpaper();
                                    saveImageToAppPrivateFile(currentWallpaper, filename);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Bitmap extractWallpaper() {
        //final int MAX_WALLPAPER_EXTRACTION_AREA = 112 * 112;
        Drawable drawable = null;
        Bitmap bitmap = null;

        WallpaperManager wm = WallpaperManager.getInstance(this);
        WallpaperInfo info = wm.getWallpaperInfo();
        if (info != null) {
            Log.i("WALLPAPER INFO", info.toString());
            // For live wallpaper, extract colors from thumbnail
            drawable = info.loadThumbnail(getPackageManager());
        } else {
            if (Utilities.ATLEAST_NOUGAT) {
                hasExternalStoragePermission(this);
                try (ParcelFileDescriptor fd = wm.getWallpaperFile(FLAG_SYSTEM)) {
                    bitmap = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor());
                /*
                BitmapRegionDecoder decoder = BitmapRegionDecoder
                        .newInstance(fd.getFileDescriptor(), false);

                int requestedArea = decoder.getWidth() * decoder.getHeight();
                BitmapFactory.Options options = new BitmapFactory.Options();

                if (requestedArea > MAX_WALLPAPER_EXTRACTION_AREA) {
                    double areaRatio =
                            (double) requestedArea / MAX_WALLPAPER_EXTRACTION_AREA;
                    double nearestPowOf2 =
                            Math.floor(Math.log(areaRatio) / (2 * Math.log(2)));
                    options.inSampleSize = (int) Math.pow(2, nearestPowOf2);
                }
                Rect region = new Rect(0, 0, decoder.getWidth(), decoder.getHeight());
                bitmap = decoder.decodeRegion(region, options);
                decoder.recycle();
                */
                } catch (IOException | RuntimeException e) {
                    Log.e(TAG, "Fetching partial bitmap failed, trying old method", e);
                }
            }
            if(bitmap == null) {
                try {
                    drawable = wm.getDrawable();
                } catch (RuntimeException e) {
                    Log.e(TAG, "Failed to extract the wallpaper drawable", e);
                }
            }
        }

        if (drawable != null && drawable.getIntrinsicWidth() > 0 && drawable.getIntrinsicHeight() > 0) {
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                bitmap = bitmapDrawable.getBitmap();
                if(bitmap != null) return bitmap;
            }

            if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }

        return bitmap;
    }

    private boolean privateFileExists(String filename) {
        try {
            File file = getFileStreamPath(filename);
            return file.exists();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void saveImageToAppPrivateFile(Bitmap imageToSave, String filename) {
        try {
            FileOutputStream out = openFileOutput(filename, Context.MODE_PRIVATE);
            imageToSave.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap getImageFromAppPrivateFile(String filename) {
        Bitmap bitmap = null;
        try {
            //TODO check if file exists
            FileInputStream in = openFileInput(filename);
            bitmap =  BitmapFactory.decodeStream(in);
            in.close();
        } catch (Exception e) {
            System.err.println("This is expected if no wallpaper was set for this profile");
            e.printStackTrace();
        }
        return bitmap;
    }

    @Override
    public void onActivityResult(
            final int requestCode, final int resultCode, final Intent data) {
        handleActivityResult(requestCode, resultCode, data);
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onActivityResult(requestCode, resultCode, data);
        }
    }

    /** @Override for MNC */
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        PendingRequestArgs pendingArgs = mPendingRequestArgs;
        if (requestCode == REQUEST_PERMISSION_CALL_PHONE && pendingArgs != null
                && pendingArgs.getRequestCode() == REQUEST_PERMISSION_CALL_PHONE) {
            setWaitingForResult(null);

            View v = null;
            CellLayout layout = getCellLayout(pendingArgs.container, pendingArgs.screenId);
            if (layout != null) {
                v = layout.getChildAt(pendingArgs.cellX, pendingArgs.cellY);
            }
            Intent intent = pendingArgs.getPendingIntent();

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivitySafely(v, intent, null);
            } else {
                // TODO: Show a snack bar with link to settings
                Toast.makeText(this, getString(R.string.msg_no_phone_permission,
                        getString(R.string.derived_app_name)), Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == CODE_OVERLAY_PERMISSION && pendingArgs != null
                && pendingArgs.getRequestCode() == CODE_OVERLAY_PERMISSION) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            }
            else {
                Toast.makeText(this, getString(R.string.msg_no_overlay_permission,
                        getString(R.string.derived_app_name)), Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode == CODE_READ_EXTERNAL_STORAGE_PERMISSION){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveCurrentWallpaper();
            }
        }
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onRequestPermissionsResult(requestCode, permissions,
                    grantResults);
        }
    }

    /**
     * Check to see if a given screen id exists. If not, create it at the end, return the new id.
     *
     * @param screenId the screen id to check
     * @return the new screen, or screenId if it exists
     */
    private long ensurePendingDropLayoutExists(long screenId) {
        CellLayout dropLayout = mWorkspace.getScreenWithId(screenId);
        if (dropLayout == null) {
            // it's possible that the add screen was removed because it was
            // empty and a re-bind occurred
            mWorkspace.addExtraEmptyScreen();
            return mWorkspace.commitExtraEmptyScreen();
        } else {
            return screenId;
        }
    }

    @Thunk void completeTwoStageWidgetDrop(
            final int resultCode, final int appWidgetId, final PendingRequestArgs requestArgs) {
        CellLayout cellLayout = mWorkspace.getScreenWithId(requestArgs.screenId);
        Runnable onCompleteRunnable = null;
        int animationType = 0;

        AppWidgetHostView boundWidget = null;
        if (resultCode == RESULT_OK) {
            animationType = Workspace.COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION;
            final AppWidgetHostView layout = mAppWidgetHost.createView(this, appWidgetId,
                    requestArgs.getWidgetHandler().getProviderInfo(this));
            boundWidget = layout;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    completeAddAppWidget(appWidgetId, requestArgs, layout, null);
                    exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED),
                            EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT, null);
                }
            };
        } else if (resultCode == RESULT_CANCELED) {
            mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            animationType = Workspace.CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION;
        }
        if (mDragLayer.getAnimatedView() != null) {
            mWorkspace.animateWidgetDrop(requestArgs, cellLayout,
                    (DragView) mDragLayer.getAnimatedView(), onCompleteRunnable,
                    animationType, boundWidget, true);
        } else if (onCompleteRunnable != null) {
            // The animated view may be null in the case of a rotation during widget configuration
            onCompleteRunnable.run();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirstFrameAnimatorHelper.setIsVisible(false);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onStop();
        }

        if (Utilities.ATLEAST_NOUGAT_MR1) {
            mAppWidgetHost.stopListening();
        }

        //Log.e("NotificationListener", "Stopped by Launcher");
        //NotificationListener.removeNotificationsChangedListener();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirstFrameAnimatorHelper.setIsVisible(true);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onStart();
        }

        if (Utilities.ATLEAST_NOUGAT_MR1) {
            mAppWidgetHost.startListening();
        }

        if (!isWorkspaceLoading()) {
            NotificationListener.setNotificationsChangedListener(mPopupDataProvider);
        }

        if (mShouldFadeInScrim && mDragLayer.getBackground() != null) {
            if (mScrimAnimator != null) {
                mScrimAnimator.cancel();
            }
            mDragLayer.getBackground().setAlpha(0);
            mScrimAnimator = ObjectAnimator.ofInt(mDragLayer.getBackground(),
                    LauncherAnimUtils.DRAWABLE_ALPHA, 0, 255);
            mScrimAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mScrimAnimator = null;
                }
            });
            mScrimAnimator.setDuration(600);
            mScrimAnimator.setStartDelay(getWindow().getTransitionBackgroundFadeDuration());
            mScrimAnimator.start();
        }
        mShouldFadeInScrim = false;
    }

    @Override
    protected void onResume() {
        long startTime = 0;
        if (DEBUG_RESUME_TIME) {
            startTime = System.currentTimeMillis();
            Log.v(TAG, "Launcher.onResume()");
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.preOnResume();
        }

        super.onResume();
        getUserEventDispatcher().resetElapsedSessionMillis();

        // Restore the previous launcher state
        if (mOnResumeState == State.WORKSPACE) {
            showWorkspace(false);
        } else if (mOnResumeState == State.APPS) {
            boolean launchedFromApp = (mWaitingForResume != null);
            // Don't update the predicted apps if the user is returning to launcher in the apps
            // view after launching an app, as they may be depending on the UI to be static to
            // switch to another app, otherwise, if it was
            showAppsView(false /* animated */, !launchedFromApp /* updatePredictedApps */,
                    false /* focusSearchBar */);
        } else if (mOnResumeState == State.WIDGETS) {
            showWidgetsView(false, false);
        }
        if (mOnResumeState != State.APPS) {
            tryAndUpdatePredictedApps();
        }
        mOnResumeState = State.NONE;

        mPaused = false;
        if (mOnResumeNeedsLoad) {
            setWorkspaceLoading(true);
            mModel.startLoader(getCurrentWorkspaceScreen());
            mOnResumeNeedsLoad = false;
        }
        if (mBindOnResumeCallbacks.size() > 0) {
            // We might have postponed some bind calls until onResume (see waitUntilResume) --
            // execute them here
            long startTimeCallbacks = 0;
            if (DEBUG_RESUME_TIME) {
                startTimeCallbacks = System.currentTimeMillis();
            }

            for (int i = 0; i < mBindOnResumeCallbacks.size(); i++) {
                mBindOnResumeCallbacks.get(i).run();
            }
            mBindOnResumeCallbacks.clear();
            if (DEBUG_RESUME_TIME) {
                Log.d(TAG, "Time spent processing callbacks in onResume: " +
                    (System.currentTimeMillis() - startTimeCallbacks));
            }
        }
        if (mOnResumeCallbacks.size() > 0) {
            for (int i = 0; i < mOnResumeCallbacks.size(); i++) {
                mOnResumeCallbacks.get(i).run();
            }
            mOnResumeCallbacks.clear();
        }

        // Reset the pressed state of icons that were locked in the press state while activities
        // were launching
        if (mWaitingForResume != null) {
            // Resets the previous workspace icon press state
            mWaitingForResume.setStayPressed(false);
        }

        // It is possible that widgets can receive updates while launcher is not in the foreground.
        // Consequently, the widgets will be inflated in the orientation of the foreground activity
        // (framework issue). On resuming, we ensure that any widgets are inflated for the current
        // orientation.
        if (!isWorkspaceLoading()) {
            getWorkspace().reinflateWidgetsIfNecessary();
        }

        if (DEBUG_RESUME_TIME) {
            Log.d(TAG, "Time spent in onResume: " + (System.currentTimeMillis() - startTime));
        }

        // We want to suppress callbacks about CustomContent being shown if we have just received
        // onNewIntent while the user was present within launcher. In that case, we post a call
        // to move the user to the main screen (which will occur after onResume). We don't want to
        // have onHide (from onPause), then onShow, then onHide again, which we get if we don't
        // suppress here.
        if (mWorkspace.getCustomContentCallbacks() != null
                && !mMoveToDefaultScreenFromNewIntent) {
            // If we are resuming and the custom content is the current page, we call onShow().
            // It is also possible that onShow will instead be called slightly after first layout
            // if PagedView#setRestorePage was set to the custom content page in onCreate().
            if (mWorkspace.isOnOrMovingToCustomContent()) {
                mWorkspace.getCustomContentCallbacks().onShow(true);
            }
        }
        mMoveToDefaultScreenFromNewIntent = false;
        updateInteraction(Workspace.State.NORMAL, mWorkspace.getState());
        mWorkspace.onResume();

        // Process any items that were added while Launcher was away.
        InstallShortcutReceiver.disableAndFlushInstallQueue(
                InstallShortcutReceiver.FLAG_ACTIVITY_PAUSED, this);

        // Refresh shortcuts if the permission changed.
        mModel.refreshShortcutsIfRequired();

        if (mAllAppsController.isTransitioning()) {
            mAppsView.setVisibility(View.VISIBLE);
        }
        if (shouldShowDiscoveryBounce()) {
            mAllAppsController.showDiscoveryBounce();
        }
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onResume();
        }

        //checkLocationPermission(this);
        hasExternalStoragePermission(this);
        checkFineLocationPermission(this);

    }

    @Override
    protected void onPause() {
        // Ensure that items added to Launcher are queued until Launcher returns
        InstallShortcutReceiver.enableInstallQueue(InstallShortcutReceiver.FLAG_ACTIVITY_PAUSED);

        super.onPause();
        mPaused = true;
        mDragController.cancelDrag();
        mDragController.resetLastGestureUpTime();

        // We call onHide() aggressively. The custom content callbacks should be able to
        // debounce excess onHide calls.
        if (mWorkspace.getCustomContentCallbacks() != null) {
            mWorkspace.getCustomContentCallbacks().onHide();
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onPause();
        }
    }

    public interface CustomContentCallbacks {
        // Custom content is completely shown. {@code fromResume} indicates whether this was caused
        // by a onResume or by scrolling otherwise.
        void onShow(boolean fromResume);

        // Custom content is completely hidden
        void onHide();

        // Custom content scroll progress changed. From 0 (not showing) to 1 (fully showing).
        void onScrollProgressChanged(float progress);

        // Indicates whether the user is allowed to scroll away from the custom content.
        boolean isScrollingAllowed();
    }

    public interface LauncherOverlay {

        /**
         * Touch interaction leading to overscroll has begun
         */
        void onScrollInteractionBegin();

        /**
         * Touch interaction related to overscroll has ended
         */
        void onScrollInteractionEnd();

        /**
         * Scroll progress, between 0 and 100, when the user scrolls beyond the leftmost
         * screen (or in the case of RTL, the rightmost screen).
         */
        void onScrollChange(float progress, boolean rtl);

        /**
         * Called when the launcher is ready to use the overlay
         * @param callbacks A set of callbacks provided by Launcher in relation to the overlay
         */
        void setOverlayCallbacks(LauncherOverlayCallbacks callbacks);
    }

    public interface LauncherOverlayCallbacks {
        void onScrollChanged(float progress);
    }

    class LauncherOverlayCallbacksImpl implements LauncherOverlayCallbacks {

        public void onScrollChanged(float progress) {
            if (mWorkspace != null) {
                mWorkspace.onOverlayScrollChanged(progress);
            }
        }
    }

    protected boolean hasSettings() {
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.hasSettings();
        } else {
            // On O and above we there is always some setting present settings (add icon to
            // home screen or icon badging). On earlier APIs we will have the allow rotation
            // setting, on devices with a locked orientation,
            return Utilities.ATLEAST_OREO || !getResources().getBoolean(R.bool.allow_rotation);
        }
    }

    public void addToCustomContentPage(View customContent,
            CustomContentCallbacks callbacks, String description) {
        mWorkspace.addToCustomContentPage(customContent, callbacks, description);
    }

    // The custom content needs to offset its content to account for the QSB
    public int getTopOffsetForCustomContent() {
        return mWorkspace.getPaddingTop();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        // Flag the loader to stop early before switching
        if (mModel.isCurrentCallbacks(this)) {
            mModel.stopLoader();
        }
        //TODO(hyunyoungs): stop the widgets loader when there is a rotation.

        return Boolean.TRUE;
    }

    // We can't hide the IME if it was forced open.  So don't bother
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mHasFocus = hasFocus;

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onWindowFocusChanged(hasFocus);
        }
    }

    private boolean acceptFilter() {
        final InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        return !inputManager.isFullscreenMode();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        final int uniChar = event.getUnicodeChar();
        final boolean handled = super.onKeyDown(keyCode, event);
        final boolean isKeyNotWhitespace = uniChar > 0 && !Character.isWhitespace(uniChar);
        if (!handled && acceptFilter() && isKeyNotWhitespace) {
            boolean gotKey = TextKeyListener.getInstance().onKeyDown(mWorkspace, mDefaultKeySsb,
                    keyCode, event);
            if (gotKey && mDefaultKeySsb != null && mDefaultKeySsb.length() > 0) {
                // something usable has been typed - start a search
                // the typed text will be retrieved and cleared by
                // showSearchDialog()
                // If there are multiple keystrokes before the search dialog takes focus,
                // onSearchRequested() will be called for every keystroke,
                // but it is idempotent, so it's fine.
                return onSearchRequested();
            }
        }

        // Eat the long press event so the keyboard doesn't come up.
        if (keyCode == KeyEvent.KEYCODE_MENU && event.isLongPress()) {
            return true;
        }

        return handled;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            // Ignore the menu key if we are currently dragging or are on the custom content screen
            if (!isOnCustomContent() && !mDragController.isDragging()) {
                // Close any open floating view
                AbstractFloatingView.closeAllOpenViews(this);

                // Stop resizing any widgets
                mWorkspace.exitWidgetResizeMode();

                // Show the overview mode if we are on the workspace
                if (mState == State.WORKSPACE && !mWorkspace.isInOverviewMode() &&
                        !mWorkspace.isSwitchingState()) {
                    mOverviewPanel.requestFocus();
                    showOverviewMode(true, true /* requestButtonFocus */);
                }
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private String getTypedText() {
        return mDefaultKeySsb.toString();
    }

    @Override
    public void clearTypedText() {
        mDefaultKeySsb.clear();
        mDefaultKeySsb.clearSpans();
        Selection.setSelection(mDefaultKeySsb, 0);
    }

    /**
     * Restores the previous state, if it exists.
     *
     * @param savedState The previous state.
     */
    private void restoreState(Bundle savedState) {
        if (savedState == null) {
            return;
        }

        int stateOrdinal = savedState.getInt(RUNTIME_STATE, State.WORKSPACE.ordinal());
        State[] stateValues = State.values();
        State state = (stateOrdinal >= 0 && stateOrdinal < stateValues.length)
                ? stateValues[stateOrdinal] : State.WORKSPACE;
        if (state == State.APPS || state == State.WIDGETS) {
            mOnResumeState = state;
        }

        PendingRequestArgs requestArgs = savedState.getParcelable(RUNTIME_STATE_PENDING_REQUEST_ARGS);
        if (requestArgs != null) {
            setWaitingForResult(requestArgs);
        }

        mPendingActivityResult = savedState.getParcelable(RUNTIME_STATE_PENDING_ACTIVITY_RESULT);
    }

    /**
     * Finds all the views we need and configure them properly.
     */
    private void setupViews() {
        mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
        mFocusHandler = mDragLayer.getFocusIndicatorHelper();
        mWorkspace = mDragLayer.findViewById(R.id.workspace);
        mWorkspace.initParentViews(mDragLayer);

        mLauncherView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        // Setup the drag layer
        mDragLayer.setup(this, mDragController, mAllAppsController);

        // Setup the hotseat
        mHotseat = (Hotseat) findViewById(R.id.hotseat);
        if (mHotseat != null) {
            mHotseat.setOnLongClickListener(this);
        }

        // Setup the overview panel
        setupOverviewPanel();

        // Setup the workspace
        mWorkspace.setHapticFeedbackEnabled(false);
        mWorkspace.setOnLongClickListener(this);
        mWorkspace.setup(mDragController);
        // Until the workspace is bound, ensure that we keep the wallpaper offset locked to the
        // default state, otherwise we will update to the wrong offsets in RTL
        mWorkspace.lockWallpaperToDefaultPage();
        mWorkspace.bindAndInitFirstWorkspaceScreen(null /* recycled qsb */);
        mDragController.addDragListener(mWorkspace);

        // Get the search/delete/uninstall bar
        mDropTargetBar = mDragLayer.findViewById(R.id.drop_target_bar);

        // Setup Apps and Widgets
        mAppsView = (AllAppsContainerView) findViewById(R.id.apps_view);
        mWidgetsView = (WidgetsContainerView) findViewById(R.id.widgets_view);

        // Setup the drag controller (drop targets have to be added in reverse order in priority)
        mDragController.setMoveTarget(mWorkspace);
        mDragController.addDropTarget(mWorkspace);
        mDropTargetBar.setup(mDragController);

        mAllAppsController.setupViews(mAppsView, mHotseat, mWorkspace);

        if (TestingUtils.MEMORY_DUMP_ENABLED) {
            TestingUtils.addWeightWatcher(this);
        }
    }

    private void setupOverviewPanel() {
        mOverviewPanel = (ViewGroup) findViewById(R.id.overview_panel);

        // Bind wallpaper button actions
        View wallpaperButton = findViewById(R.id.wallpaper_button);
        new OverviewButtonClickListener(ControlType.WALLPAPER_BUTTON) {
            @Override
            public void handleViewClick(View view) {
                onClickWallpaperPicker(view);
            }
        }.attachTo(wallpaperButton);

        // Bind widget button actions
        /*
        mWidgetsButton = findViewById(R.id.widget_button);
        new OverviewButtonClickListener(ControlType.WIDGETS_BUTTON) {
            @Override
            public void handleViewClick(View view) {
                onClickAddWidgetButton(view);
            }
        }.attachTo(mWidgetsButton);
        */

        // Bind settings actions
        View settingsButton = findViewById(R.id.settings_button);
        boolean hasSettings = hasSettings();
        if (hasSettings) {
            new OverviewButtonClickListener(ControlType.SETTINGS_BUTTON) {
                @Override
                public void handleViewClick(View view) {
                    onClickSettingsButton(view);
                }
            }.attachTo(settingsButton);
        } else {
            settingsButton.setVisibility(View.GONE);
        }

        // Bind profiles button actions
        View profilesButton = findViewById(R.id.profiles_button);
        new OverviewButtonClickListener(ControlType.PROFILES_BUTTON) {
            @Override
            public void handleViewClick(View view) {
                onClickProfilesButton(view);
            }
        }.attachTo(profilesButton);
         /***
         * Old version: text view for displaying current profile
         * new version: see ProfileTileService
         *
         *
        View profileDisplay = findViewById(R.id.profile_display);
        new OverviewButtonClickListener(ControlType.PROFILES_BUTTON) {
            @Override
            public void handleViewClick(View view) {
                new ManualProfileSelection().show(getFragmentManager(), "manual_profile_selection");
            }
        }.attachTo(profileDisplay);*/

        mOverviewPanel.setAlpha(0f);
    }

    public static Set<String> getAllProfiles() {
        Set<String> set = mSharedPrefs.getStringSet(ProfilesActivity.PROFILES_MANAGED, null);
        return set;
    }

    public static class ManualProfileSelection
            extends DialogFragment implements DialogInterface.OnClickListener {

        private String[] mItems;
        private Launcher mLauncher;

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            try {
                mLauncher = (Launcher) context;
                mItems = new String[]{
                        context.getString(R.string.profile_home),
                        context.getString(R.string.profile_work),
                        context.getString(R.string.profile_default)
                };
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString()
                        + " must be Launcher.class");
            }

        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            return new AlertDialog.Builder(context)
                    .setTitle(R.string.title_manually_select_profile)
                    .setItems(mItems, this)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            String[] profiles = {"home", "work", "default"};
            Log.d("PROFILE_UPDATE", "From dialog");
            //mLauncher.mSharedPrefs.edit().putString(MANUAL_PROFILE_PREF, profiles[which]).commit();
            //mLauncher.updateProfile(profiles[which]);
        }
    }

    public static void updateSharedPrefsProfile(String profile){
        mSharedPrefs.edit().putString(MANUAL_PROFILE_PREF, profile).apply();
        String profileName = profile.split("_")[0];
        mSharedPrefs.edit().putString(CURRENT_PROFILE_PREF, profileName).apply();
    }

    /**
     * Sets the all apps button. This method is called from {@link Hotseat}.
     * TODO: Get rid of this.
     */
    public void setAllAppsButton(View allAppsButton) {
        mAllAppsButton = allAppsButton;
    }

    public View getStartViewForAllAppsRevealAnimation() {
        return FeatureFlags.NO_ALL_APPS_ICON ? mWorkspace.getPageIndicator() : mAllAppsButton;
    }

    public View getWidgetsButton() {
        return mWidgetsButton;
    }

    /**
     * Creates a view representing a shortcut.
     *
     * @param info The data structure describing the shortcut.
     */
    View createShortcut(ShortcutInfo info) {
        return createShortcut((ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentPage()), info);
    }

    /**
     * Creates a view representing a shortcut inflated from the specified resource.
     *
     * @param parent The group the shortcut belongs to.
     * @param info The data structure describing the shortcut.
     *
     * @return A View inflated from layoutResId.
     */
    public View createShortcut(ViewGroup parent, ShortcutInfo info) {
        BubbleTextView favorite = (BubbleTextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_icon, parent, false);
        favorite.applyFromShortcutInfo(info);
        favorite.setOnClickListener(this);
        favorite.setOnFocusChangeListener(mFocusHandler);
        Set<String> appsOnHomescreen = mSharedPrefs.getStringSet(APPS_ON_HOMESCREEN, null);

        String currentProfile = mSharedPrefs.getString(CURRENT_PROFILE_PREF, null);

        if(info.container == Favorites.CONTAINER_DESKTOP || info.container == -1){
            if(appsOnHomescreen!=null){
                ArrayList<String> appsOnHomescreenList = new ArrayList<>(appsOnHomescreen);
                String entryToAdd = "";
                String entryToDelete = "";
                boolean isEntryToDelete = false;
                ArrayList<String> profileNames = new ArrayList<>();
                // search if the app is already saved in the list, if not than add it to the list
                for(String profileApps : appsOnHomescreenList){
                    String profileName = profileApps.split("_")[0];
                    if(currentProfile.equals("home")||currentProfile.equals("work")||currentProfile.equals("disconnected")||currentProfile.equals("default")){
                        //do nothing
                    } else {
                        for(String newAddedProfile : newAddedProfiles){
                            if(newAddedProfile.substring(1).equals(currentProfile)){
                                currentProfile = newAddedProfile.charAt(0)+"";
                            }
                        }
                    }
                    profileNames.add(profileName);
                    if(profileName.equals(currentProfile)){
                        //if profile has already entries, save new entry
                        if(profileApps.split("_").length>1){
                            //if profile has more than one entry
                            if(profileApps.contains(",")){
                                List<String> apps = Arrays.asList(profileApps.split("_")[1].split(","));
                                for(String app : apps){
                                    if(!apps.contains(info.title.toString())){
                                        String updatedProfileApps = profileApps+","+info.title.toString();
                                        Log.d("---", "updated profile apps: "+updatedProfileApps);
                                        isEntryToDelete = true;
                                        entryToDelete = profileApps;
                                        entryToAdd = updatedProfileApps;
                                    }
                                }
                            } else {
                                //if profile has only one entry
                                isEntryToDelete = true;
                                entryToDelete = profileApps;
                                entryToAdd = profileApps+","+info.title.toString();
                            }

                        } else {
                            String updatedProfileApps = profileApps+info.title.toString();
                            appsOnHomescreenList.remove(profileApps);
                            appsOnHomescreenList.add(updatedProfileApps);
                            Set<String> set = new HashSet(appsOnHomescreenList);
                            mSharedPrefs.edit().putStringSet(APPS_ON_HOMESCREEN, set).apply();
                        }
                    }
                }
                if(!profileNames.contains(currentProfile)) {
                    //if profile is not saved in the list
                    String newProfileApps = currentProfile+"_"+info.title.toString();
                    appsOnHomescreenList.add(newProfileApps);
                    Log.d("---", "new list: "+appsOnHomescreenList);
                    Set<String> set = new HashSet(appsOnHomescreenList);
                    mSharedPrefs.edit().putStringSet(APPS_ON_HOMESCREEN, set).apply();
                }
                if(isEntryToDelete){
                    appsOnHomescreenList.remove(entryToDelete);
                    appsOnHomescreenList.add(entryToAdd);
                    Log.d("---", "updated list: "+appsOnHomescreenList);
                    isEntryToDelete = false;
                    Set<String> set = new HashSet(appsOnHomescreenList);
                    mSharedPrefs.edit().putStringSet(APPS_ON_HOMESCREEN, set).apply();
                }

            } else {
                //if list is null
                ArrayList<String> appsOnHomescreenList = new ArrayList<>();
                String newProfileApps = "";
                if(currentProfile.equals("home")||currentProfile.equals("work")||currentProfile.equals("default")||currentProfile.equals("disconnected")){
                    newProfileApps = currentProfile+"_"+info.title.toString();
                } else {
                    for(String newAddedProfile : newAddedProfiles){
                        if(newAddedProfile.substring(1).equals(currentProfile)){
                            newProfileApps = newAddedProfile.charAt(0)+"_"+info.title.toString();
                        }
                    }
                }
                appsOnHomescreenList.add(newProfileApps);
                Log.d("---", "totally new list: "+appsOnHomescreenList);
                Set<String> set = new HashSet(appsOnHomescreenList);
                mSharedPrefs.edit().putStringSet(APPS_ON_HOMESCREEN, set).apply();
            }
            fetchHomescreenAppList();
            if(currentProfile.equals("home")||currentProfile.equals("work")||currentProfile.equals("disconnected")||currentProfile.equals("default")){
                LogEntryProfileEdited logEntry = new LogEntryProfileEdited(currentProfile, "app added on homescreen", getSSIDPref(currentProfile), getSchedulePref(currentProfile), getRingtonePref(currentProfile), getNotificationSoundPref(currentProfile), getNotificationBlockedPref(currentProfile), getMinimalDesignPref(currentProfile), getHomeScreenAppsList(currentProfile), getWallpaperInfo(currentProfile), getGrayScalePref(currentProfile));
                firebaseLogger.addLogMessage("events", "profile edited", logEntry);
            } else {
                Set set = mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
                if(set!=null){
                    ArrayList<String> newAddedProfiles = new ArrayList<>(set);
                    for(String newAddedProfile: newAddedProfiles){
                        if(newAddedProfile.substring(1).equals(currentProfile) || (newAddedProfile.charAt(0)+"").equals(currentProfile)){
                            String profile = newAddedProfile.charAt(0)+"";
                            LogEntryProfileEdited logEntry = new LogEntryProfileEdited(newAddedProfile.substring(1), "app added on homescreen", getSSIDPref(profile), getSchedulePref(profile), getRingtonePref(profile), getNotificationSoundPref(profile), getNotificationBlockedPref(profile), getMinimalDesignPref(profile), getHomeScreenAppsList(profile), getWallpaperInfo(profile), getGrayScalePref(profile));
                            firebaseLogger.addLogMessage("events", "profile edited", logEntry);
                        }
                    }
                }
            }

        }
        return favorite;
    }

    /**
     * Add a shortcut to the workspace or to a Folder.
     *
     * @param data The intent describing the shortcut.
     */
    private void completeAddShortcut(Intent data, long container, long screenId, int cellX,
            int cellY, PendingRequestArgs args) {
        if (args.getRequestCode() != REQUEST_CREATE_SHORTCUT
                || args.getPendingIntent().getComponent() == null) {
            return;
        }

        int[] cellXY = mTmpAddItemCellCoordinates;
        CellLayout layout = getCellLayout(container, screenId);

        ShortcutInfo info = null;
        if (Utilities.ATLEAST_OREO) {
            info = LauncherAppsCompatVO.createShortcutInfoFromPinItemRequest(
                    this, LauncherAppsCompatVO.getPinItemRequest(data), 0);
        }

        if (info == null) {
            // Legacy shortcuts are only supported for primary profile.
            info = Process.myUserHandle().equals(args.user)
                    ? InstallShortcutReceiver.fromShortcutIntent(this, data) : null;

            if (info == null) {
                Log.e(TAG, "Unable to parse a valid custom shortcut result");
                return;
            } else if (!new PackageManagerHelper(this).hasPermissionForActivity(
                    info.intent, args.getPendingIntent().getComponent().getPackageName())) {
                // The app is trying to add a shortcut without sufficient permissions
                Log.e(TAG, "Ignoring malicious intent " + info.intent.toUri(0));
                return;
            }
        }

        if (container < 0) {
            // Adding a shortcut to the Workspace.
            final View view = createShortcut(info);
            boolean foundCellSpan = false;
            // First we check if we already know the exact location where we want to add this item.
            if (cellX >= 0 && cellY >= 0) {
                cellXY[0] = cellX;
                cellXY[1] = cellY;
                foundCellSpan = true;

                // If appropriate, either create a folder or add to an existing folder
                if (mWorkspace.createUserFolderIfNecessary(view, container, layout, cellXY, 0,
                        true, null, null)) {
                    return;
                }
                DragObject dragObject = new DragObject();
                dragObject.dragInfo = info;
                if (mWorkspace.addToExistingFolderIfNecessary(view, layout, cellXY, 0, dragObject,
                        true)) {
                    return;
                }
            } else {
                foundCellSpan = layout.findCellForSpan(cellXY, 1, 1);
            }

            if (!foundCellSpan) {
                mWorkspace.onNoCellFound(layout);
                return;
            }

            getModelWriter().addItemToDatabase(info, container, screenId, cellXY[0], cellXY[1]);
            mWorkspace.addInScreen(view, info);
        } else {
            // Adding a shortcut to a Folder.
            FolderIcon folderIcon = findFolderIcon(container);
            if (folderIcon != null) {
                FolderInfo folderInfo = (FolderInfo) folderIcon.getTag();
                folderInfo.add(info, args.rank, false);
            } else {
                Log.e(TAG, "Could not find folder with id " + container + " to add shortcut.");
            }
        }
    }

    public FolderIcon findFolderIcon(final long folderIconId) {
        return (FolderIcon) mWorkspace.getFirstMatch(new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View view) {
                return info != null && info.id == folderIconId;
            }
        });
    }

    /**
     * Add a widget to the workspace.
     *
     * @param appWidgetId The app widget id
     */
    @Thunk void completeAddAppWidget(int appWidgetId, ItemInfo itemInfo,
            AppWidgetHostView hostView, LauncherAppWidgetProviderInfo appWidgetInfo) {

        if (appWidgetInfo == null) {
            appWidgetInfo = mAppWidgetManager.getLauncherAppWidgetInfo(appWidgetId);
        }

        if (appWidgetInfo.isCustomWidget) {
            appWidgetId = LauncherAppWidgetInfo.CUSTOM_WIDGET_ID;
        }

        LauncherAppWidgetInfo launcherInfo;
        launcherInfo = new LauncherAppWidgetInfo(appWidgetId, appWidgetInfo.provider);
        launcherInfo.spanX = itemInfo.spanX;
        launcherInfo.spanY = itemInfo.spanY;
        launcherInfo.minSpanX = itemInfo.minSpanX;
        launcherInfo.minSpanY = itemInfo.minSpanY;
        launcherInfo.user = appWidgetInfo.getUser();

        getModelWriter().addItemToDatabase(launcherInfo,
                itemInfo.container, itemInfo.screenId, itemInfo.cellX, itemInfo.cellY);

        if (hostView == null) {
            // Perform actual inflation because we're live
            hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
        }
        hostView.setVisibility(View.VISIBLE);
        prepareAppWidget(hostView, launcherInfo);
        mWorkspace.addInScreen(hostView, launcherInfo);
    }

    private void prepareAppWidget(AppWidgetHostView hostView, LauncherAppWidgetInfo item) {
        hostView.setTag(item);
        item.onBindAppWidget(this, hostView);
        hostView.setFocusable(true);
        hostView.setOnFocusChangeListener(mFocusHandler);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            String currentProfile = mSharedPrefs.getString(CURRENT_PROFILE_PREF, "default");
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mDragLayer.clearResizeFrame();
                if(usedApps.isEmpty()){
                    usedApps.add("empty");
                }
                if(usedShortcuts.isEmpty()){
                    usedShortcuts.add("empty");
                }
                LogEntryUnlocks logEntry = new LogEntryUnlocks(currentProfile, usedApps, usedShortcuts);
                firebaseLogger.addLogMessage("unlocks", "screen off", logEntry);
                usedApps = new ArrayList<>();
                usedShortcuts = new ArrayList<>();

                // Reset AllApps to its initial state only if we are not in the middle of
                // processing a multi-step drop
                if (mAppsView != null && mWidgetsView != null && mPendingRequestArgs == null) {
                    if (!showWorkspace(false)) {
                        // If we are already on the workspace, then manually reset all apps
                        mAppsView.reset();
                    }
                }
                mShouldFadeInScrim = true;
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                // ACTION_USER_PRESENT is sent after onStart/onResume. This covers the case where
                // the user unlocked.
                LogEntryUnlocks logEntry = new LogEntryUnlocks(currentProfile, null, null);
                firebaseLogger.addLogMessage("unlocks", "phone unlocked", logEntry);
                mShouldFadeInScrim = false;
            } else if(Intent.ACTION_SCREEN_ON.equals(action)){
                LogEntryUnlocks logEntry = new LogEntryUnlocks(currentProfile, null, null);
                firebaseLogger.addLogMessage("unlocks", "screen on", logEntry);
            }
        }
    };

    /*
    private final BroadcastReceiver mAlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        }
    };*/

    private final BroadcastReceiver mWiFiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!isRecreatedForThemeChange){
                String newWifiConnection = null;
                //String toastMsg = null;

                boolean airplaneMode = Settings.Global.getInt(context.getContentResolver(),
                        Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
                Log.d("WIFI_RECEIVER", "airplane mode is "+(airplaneMode ? "on" : "off"));
                if (airplaneMode) {
                    newWifiConnection = "disconnected";
                } else if (intent.getAction().equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                    if (!airplaneMode) {
                        newWifiConnection = getCurrentSSID(context);
                        if (newWifiConnection == null) newWifiConnection = "default";
                    }
                } else if (intent.getAction().equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                    boolean connected = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);
                    Log.d("WIFI_RECEIVER", "wifi is "+(connected ? "connected" : "disconnected"));
                    if(!connected) {
                        //Start service for disconnected state here
                        //toastMsg = "WiFi disconnected";
                        newWifiConnection = "default";
                    }
                } else if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    Log.d("WIFI_RECEIVER", "Wifi state changed! Trying to retrieve network SSID...");
                    if (netInfo != null && netInfo.isConnected()) {
                        //Start service for connected state here.
                        newWifiConnection = getCurrentSSID(context);
                        //Log.e("WIFI_RECEIVER", "Found network SSID "+newWifiConnection);
                    /*
                    WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (wifiManager != null) {
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        String ssid = wifiInfo.getSSID().replaceAll("\"", "");
                        //toastMsg = "SSID: " + ssid;
                        //TODO multiple SSIDs per profile possible!
                        newWifiConnection = ssid;
                    }
                    */
                    } else {
                        newWifiConnection = "default";
                        Log.d("WIFI_RECEIVER", "Failed.");
                    }
                }

                if (newWifiConnection != null && context instanceof Launcher) {
                    changeProfile(newWifiConnection);
                    //if (toastMsg != null && !((Launcher) context).isFinishing()) Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show();
                }

            /*
            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if(info != null && info.isConnected()) {
                disconnected = false;
                // Do your work.

                // e.g. To check the Network Name or other info:
                WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ssid = wifiInfo.getSSID();
                Toast.makeText(context, "SSID: "+ssid, Toast.LENGTH_LONG).show();

                //TODO multiple SSIDs per profile possible!
            } else {
                if (!disconnected) {
                    disconnected = true;
                    Toast.makeText(context, "WiFi disconnected", Toast.LENGTH_LONG).show();
                }
            }
            */
            } else {
                isRecreatedForThemeChange = false;
            }

        }
    };

    private String getCurrentSSID(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                return wifiInfo.getSSID().replaceAll("\"", "");
            }
        } catch (Exception e) {
            Log.e("Get Wifi Manager", "Something went wrong while fetching current SSID:");
            e.printStackTrace();
        }
        return null;
    }

    public boolean changeProfile(String newSSID) {
        if (newSSID == null) return false;
        newSSID = newSSID.trim();
        Log.d("NEW SSID", newSSID);

        //TODO disconnected should only fire if no connection to the internet exists (no LTE!!)
        if (newSSID.equals("disconnected")){
            LogEntryProfileTriggered logEntry = new LogEntryProfileTriggered("disconnected", "airplane mode on", newSSID);
            firebaseLogger.addLogMessage("events", "profile triggered", logEntry);
            return updateProfile("disconnected");
        }

        String[] work_ssids = mSharedPrefs.getString("work_ssids", "").split("\\n");
        for (String ssid : work_ssids) {
            if (ssid.trim().equals(newSSID)){
                LogEntryProfileTriggered logEntry = new LogEntryProfileTriggered("work", "wifi changed", ssid);
                firebaseLogger.addLogMessage("events", "profile triggered", logEntry);
                return updateProfile("work");
            }
        }

        String[] home_ssids = mSharedPrefs.getString("home_ssids", "").split("\\n");

        for (String ssid : home_ssids) {
            if (ssid.equals(newSSID)){
                LogEntryProfileTriggered logEntry = new LogEntryProfileTriggered("home", "wifi changed", ssid);
                firebaseLogger.addLogMessage("events", "profile triggered", logEntry);
                return updateProfile("home");
            }
        }
        if(mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null)!=null){
            newAddedProfiles = new ArrayList<>(mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null));
            if(newAddedProfiles!=null){
                for(String newProfile : newAddedProfiles){
                    String profileID = newProfile.charAt(0)+"";
                    String[] profile_ssids = mSharedPrefs.getString(profileID+"_ssids", "").split("\\n");
                    for(String ssid : profile_ssids) {
                        if(ssid.equals(newSSID)){
                            LogEntryProfileTriggered logEntry = new LogEntryProfileTriggered(newProfile.substring(1), "wifi changed", ssid);
                            firebaseLogger.addLogMessage("events", "profile triggered", logEntry);
                            return updateProfile(newProfile.substring(1));
                        }
                    }
                }
            }
        }
        LogEntryProfileTriggered logEntry = new LogEntryProfileTriggered("default", "wifi changed", "no ssid matches");
        firebaseLogger.addLogMessage("events", "profile triggered", logEntry);
        return updateProfile("default");
    }

    private static boolean firstTime = true;
    private String lastProfileUpdate = null;
    public boolean updateProfile(String profile) {
        if (profile == null || profile.isEmpty()) return false;

        String manualProfile = mSharedPrefs.getString(MANUAL_PROFILE_PREF, null);
        if(manualProfile!=null){
            manualProfile = manualProfile.split("_")[0];
        }
        if(firstTime && manualProfile != null) {
            // this happens if a profile was manually changed to a profile with a different theme which triggered a recreate()
            mSharedPrefs.edit().putString(MANUAL_PROFILE_PREF, null).apply();
            firstTime = false;

            //profile = manualProfile;
            lastProfileUpdate = manualProfile;
            return false;
        }
        firstTime = false;

        if (mSharedPrefs.getString(CURRENT_PROFILE_PREF, "").equals(profile)) return true;

        mSharedPrefs.edit().putString(CURRENT_PROFILE_PREF, profile).apply();
        
        Log.d("LAST PROFILE UPDATE", (lastProfileUpdate == null) ? "null" : lastProfileUpdate);
        if (profile.equals(lastProfileUpdate)) return true; /* abort updating */
        else lastProfileUpdate = profile;
        Log.d("UPDATE PROFILE", profile);
        //updateProfileDisplay(profile);
        updateWallpaper(profile);
        updateRingtone(profile);
        updateNotificationSound(profile);
        updateApps(profile);
        updateLayoutDesign(profile);

        return true;
    }

    private void updateApps(String profile) {
        //mModel.refreshAndBindWidgetsAndShortcuts(null);
        Log.e("UPDATE APPS", "With profile "+ profile);
        mModel.forceReload();
        //TODO try recreate(); Be aware that this re-trigger the wifi receiver and might complicate things!
    }

    /**
     *
     * @param profile
     * private void updateProfileDisplay(String profile) {
     *         Integer profileNameId = ProfilesActivity.ProfilesSettingsFragment.resourceIdForProfileName.get(profile);
     *         String profileName = getString(profileNameId != null ? profileNameId : R.string.profile_default);
     *
     *         TextView profileDisplay = mLauncherView.findViewById(R.id.profile_display);
     *         if (profileDisplay != null) profileDisplay.setText(profileName);
     *     }
     */
        int mColorThemeDark = Color.parseColor("#4f4f4f");
        int mColorThemeLight = Color.parseColor("#4285f4");
    /**
     *
     *     public void setProfileDisplayTheme(boolean darkTheme) {
     *         int color = darkTheme ? mColorThemeDark : mColorThemeLight;
     *         TextView profileDisplay = mLauncherView.findViewById(R.id.profile_display);
     *         profileDisplay.setTextColor(color);
     *
     *         LayerDrawable background = (LayerDrawable) profileDisplay.getBackground();
     *         GradientDrawable rectangle = (GradientDrawable) background.findDrawableByLayerId(R.id.indicator_menu_background);
     *         rectangle.setStroke(1, color);
     *
     *         VectorDrawable icon = (VectorDrawable) background.findDrawableByLayerId(R.id.indicator_menu_icon);
     *         icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
     *     }
     */

    private void updateLayoutDesign(final String profile) {
        if(profile.equals("home") || profile.equals("work") || profile.equals("default") ||profile.equals("disconnected")){
            boolean b = mSharedPrefs.getBoolean(profile + ProfilesActivity.MINIMAL_DESIGN_PREF, false);
            switchToMinimalLayout(b);
        } else{
            Set set = mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
            if(set!=null){
                newAddedProfiles = new ArrayList<>(set);
                if(newAddedProfiles!=null){
                    for(String sub : newAddedProfiles){
                        if(sub.substring(1).equals(profile)){
                            String profileID = sub.charAt(0)+"";
                            boolean b = mSharedPrefs.getBoolean(profileID + ProfilesActivity.MINIMAL_DESIGN_PREF, false);
                            switchToMinimalLayout(b);
                        }
                    }
                }
            }
        }
    }

    private void updateWallpaper(final String profile) {
        new AsyncTask<Void, Void, Void>() {
            public Void doInBackground(Void ... args) {
                try {
                    Bitmap wallpaper = getImageFromAppPrivateFile("wallpaper_"+profile);
                    if(wallpaper != null) {
                        WallpaperManager.getInstance(Launcher.this).setBitmap(wallpaper);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            WallpaperManager.getInstance(Launcher.this).setBitmap(wallpaper, null, true, WallpaperManager.FLAG_LOCK);
                        }
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR);
    }

    private void updateRingtone(final String profile) {
        new AsyncTask<Void, Void, Void>() {
            public Void doInBackground(Void ... args) {
                try {
                    String ringtone = null;
                    if(profile.equals("home") || profile.equals("work") || profile.equals("default") ||profile.equals("disconnected")){
                        ringtone = mSharedPrefs.getString(profile + "_ringtone", null);
                    } else {
                        Set set = mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
                        if(set!=null){
                            newAddedProfiles = new ArrayList<>(set);
                            if(newAddedProfiles!=null){
                                for(String sub : newAddedProfiles){
                                    if(sub.substring(1).equals(profile)){
                                        ringtone = mSharedPrefs.getString(sub.charAt(0) + "_ringtone", null);
                                    }
                                }
                            }
                        }
                    }
                    if(ringtone != null) {
                        Uri ringtoneUri = Uri.parse(ringtone);
                        if(ringtoneUri != null) setRingtone(ringtoneUri, Launcher.this, RingtoneManager.TYPE_RINGTONE);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR);
    }

    private void updateNotificationSound(final String profile) {
        new AsyncTask<Void, Void, Void>() {
            public Void doInBackground(Void ... args) {
                try {
                    String sound = null;
                    if(profile.equals("home") || profile.equals("work") || profile.equals("default") ||profile.equals("disconnected") ){
                        sound = mSharedPrefs.getString(profile + "_notification_sound", null);
                    } else {
                        Set set = mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
                        if(set!=null){
                            newAddedProfiles = new ArrayList<>(set);
                            if(newAddedProfiles!=null){
                                for(String sub : newAddedProfiles){
                                    if(sub.substring(1).equals(profile)){
                                        sound = mSharedPrefs.getString(sub.charAt(0) + "_notification_sound", null);
                                    }
                                }
                            }
                        }
                    }
                    if(sound != null) {
                        Uri notificationSoundUri = Uri.parse(sound);
                        if(notificationSoundUri != null) setRingtone(notificationSoundUri, Launcher.this, RingtoneManager.TYPE_NOTIFICATION);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR);
    }

    public static void setRingtone(Uri soundUri, Activity context, int type) {
        if (soundUri != null && TextUtils.isEmpty(soundUri.toString())) soundUri = null;
        try {
            boolean permission = hasWritePermission(context, false);
            if (permission) {
                RingtoneManager.setActualDefaultRingtoneUri(context, type, soundUri);
            }
        } catch(Exception e) {
            Log.e("SET RINGTONE", "Failed:");
            e.printStackTrace();
        }
    }

    public static boolean isAccessibilityEnabled(Context context) {
        int color_correction_enabled = 0;
        try { color_correction_enabled = Settings.Secure.getInt(context.getContentResolver(), "accessibility_display_daltonizer_enabled");

        } catch (Exception e) {
            color_correction_enabled = 0; // means default false
        }
        if(color_correction_enabled == 0 ){
            return false;
        }
        else {
            return true;
        }
    }

    /** Opens the accessibility settings for manually enabling or disabling grayscale mode
     * Accessibility > Vision > Colour Adjustment > ON > Greyscale */
    public static void changeGrayscaleSetting(Activity context) {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            context.startActivity(intent);
            /**
             * Solution to draw a gray semi-transparent overlay
             * boolean permission = hasOverlayPermission(context, true);
             * if (permission) {
             *      context.startService(new Intent(context, OverlayService.class));
            } */
        } catch(Exception e) {
            Log.e("SET GRAYSCALE MODE", "Failed:");
            e.printStackTrace();
        }
    }

    public static final int CODE_OVERLAY_PERMISSION = 43;
    private static boolean hasOverlayPermission(Activity context, boolean b) {
        boolean permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permission = Settings.canDrawOverlays(context);
        } else {
            permission = ContextCompat.checkSelfPermission(context, Manifest.permission.SYSTEM_ALERT_WINDOW) == PackageManager.PERMISSION_GRANTED;
        }
        if(permission){
            return true;
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivityForResult(intent, CODE_OVERLAY_PERMISSION);
            }
            else {
                ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.SYSTEM_ALERT_WINDOW}, CODE_OVERLAY_PERMISSION);
            }
        }
        return false;
    }

    public static final int CODE_ACCESS_LOCATION_PERMISSION = 49;
    public static void checkLocationPermission(Activity context){
        if(ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(context, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, CODE_ACCESS_LOCATION_PERMISSION);
        }
    }

    public static final int CODE_ACCESS_FINE_LOCATION_PERMISSION = 54;
    public static void checkFineLocationPermission(Activity context){
        if(ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(context, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, CODE_ACCESS_FINE_LOCATION_PERMISSION);
        }
    }

    public static final int CODE_READ_EXTERNAL_STORAGE_PERMISSION = 52;
    public static boolean hasExternalStoragePermission(Activity context){
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, CODE_READ_EXTERNAL_STORAGE_PERMISSION);
            return false;
        } else {
            return true;
        }
    }

    public static final int CODE_WRITE_SETTINGS_PERMISSION = 42;
    public static boolean hasWritePermission(Activity context, boolean ask){
        boolean permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permission = Settings.System.canWrite(context);
        } else {
            permission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
        }
        if (permission) {
            return true;
        }  else {
            if (ask) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    context.startActivityForResult(intent, CODE_WRITE_SETTINGS_PERMISSION);
                } else {
                    ActivityCompat.requestPermissions(context, new String[]{android.Manifest.permission.WRITE_SETTINGS}, CODE_WRITE_SETTINGS_PERMISSION);
                }
            }
        }
        return false;
    }

    public void updateIconBadges(final Set<PackageUserKey> updatedBadges) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                mWorkspace.updateIconBadges(updatedBadges);
                mAppsView.updateIconBadges(updatedBadges);

                PopupContainerWithArrow popup = PopupContainerWithArrow.getOpen(Launcher.this);
                if (popup != null) {
                    popup.updateNotificationHeader(updatedBadges);
                }
            }
        };
        if (!waitUntilResume(r)) {
            r.run();
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        FirstFrameAnimatorHelper.initializeDrawListener(getWindow().getDecorView());
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onAttachedToWindow();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onDetachedFromWindow();
        }
    }

    public void onWindowVisibilityChanged(int visibility) {
        // The following code used to be in onResume, but it turns out onResume is called when
        // you're in All Apps and click home to go to the workspace. onWindowVisibilityChanged
        // is a more appropriate event to handle
        if (visibility == View.VISIBLE) {
            if (!mWorkspaceLoading) {
                final ViewTreeObserver observer = mWorkspace.getViewTreeObserver();
                // We want to let Launcher draw itself at least once before we force it to build
                // layers on all the workspace pages, so that transitioning to Launcher from other
                // apps is nice and speedy.
                observer.addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
                    private boolean mStarted = false;
                    public void onDraw() {
                        if (mStarted) return;
                        mStarted = true;
                        // We delay the layer building a bit in order to give
                        // other message processing a time to run.  In particular
                        // this avoids a delay in hiding the IME if it was
                        // currently shown, because doing that may involve
                        // some communication back with the app.
                        mWorkspace.postDelayed(mBuildLayersRunnable, 500);
                        final ViewTreeObserver.OnDrawListener listener = this;
                        mWorkspace.post(new Runnable() {
                            public void run() {
                                if (mWorkspace != null &&
                                        mWorkspace.getViewTreeObserver() != null) {
                                    mWorkspace.getViewTreeObserver().
                                            removeOnDrawListener(listener);
                                }
                            }
                        });
                    }
                });
            }
            clearTypedText();
        }
    }

    public DragLayer getDragLayer() {
        return mDragLayer;
    }

    public AllAppsContainerView getAppsView() {
        return mAppsView;
    }

    public WidgetsContainerView getWidgetsView() {
        return mWidgetsView;
    }

    public Workspace getWorkspace() {
        return mWorkspace;
    }

    public Hotseat getHotseat() {
        return mHotseat;
    }

    public ViewGroup getOverviewPanel() {
        return mOverviewPanel;
    }

    public DropTargetBar getDropTargetBar() {
        return mDropTargetBar;
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    public ModelWriter getModelWriter() {
        return mModelWriter;
    }

    public SharedPreferences getSharedPrefs() {
        return mSharedPrefs;
    }

    public int getOrientation() { return mOrientation; }

    @Override
    protected void onNewIntent(Intent intent) {
        long startTime = 0;
        if (DEBUG_RESUME_TIME) {
            startTime = System.currentTimeMillis();
        }
        super.onNewIntent(intent);

        boolean alreadyOnHome = mHasFocus && ((intent.getFlags() &
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

        // Check this condition before handling isActionMain, as this will get reset.
        boolean shouldMoveToDefaultScreen = alreadyOnHome &&
                mState == State.WORKSPACE && AbstractFloatingView.getTopOpenView(this) == null;

        boolean isActionMain = Intent.ACTION_MAIN.equals(intent.getAction());
        if (isActionMain) {
            if (mWorkspace == null) {
                // Can be cases where mWorkspace is null, this prevents a NPE
                return;
            }

            // Note: There should be at most one log per method call. This is enforced implicitly
            // by using if-else statements.
            UserEventDispatcher ued = getUserEventDispatcher();

            // TODO: Log this case.
            mWorkspace.exitWidgetResizeMode();

            AbstractFloatingView topOpenView = AbstractFloatingView.getTopOpenView(this);
            if (topOpenView instanceof PopupContainerWithArrow) {
                ued.logActionCommand(Action.Command.HOME_INTENT,
                        topOpenView.getExtendedTouchView(), ContainerType.DEEPSHORTCUTS);
            } else if (topOpenView instanceof Folder) {
                ued.logActionCommand(Action.Command.HOME_INTENT,
                            ((Folder) topOpenView).getFolderIcon(), ContainerType.FOLDER);
            } else if (alreadyOnHome) {
                ued.logActionCommand(Action.Command.HOME_INTENT,
                        mWorkspace.getState().containerType, mWorkspace.getCurrentPage());
            }

            // In all these cases, only animate if we're already on home
            AbstractFloatingView.closeAllOpenViews(this, alreadyOnHome);
            exitSpringLoadedDragMode();

            // If we are already on home, then just animate back to the workspace,
            // otherwise, just wait until onResume to set the state back to Workspace
            if (alreadyOnHome) {
                if (!mAllAppsController.isDragging()) {
                    showWorkspace(true);
                }
            } else {
                mOnResumeState = State.WORKSPACE;
            }

            final View v = getWindow().peekDecorView();
            if (v != null && v.getWindowToken() != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(
                        INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }

            // Reset the apps view
            if (!alreadyOnHome && mAppsView != null) {
                mAppsView.reset();
            }

            // Reset the widgets view
            if (!alreadyOnHome && mWidgetsView != null) {
                mWidgetsView.scrollToTop();
            }

            if (mLauncherCallbacks != null) {
                mLauncherCallbacks.onHomeIntent();
            }
        }
        PinItemDragListener.handleDragRequest(this, intent);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onNewIntent(intent);
        }

        // Defer moving to the default screen until after we callback to the LauncherCallbacks
        // as slow logic in the callbacks eat into the time the scroller expects for the snapToPage
        // animation.
        if (isActionMain) {
            boolean callbackAllowsMoveToDefaultScreen =
                mLauncherCallbacks == null || mLauncherCallbacks
                    .shouldMoveToDefaultScreenOnHomeIntent();
            if (shouldMoveToDefaultScreen && !mWorkspace.isTouchActive()
                    && callbackAllowsMoveToDefaultScreen) {

                // We use this flag to suppress noisy callbacks above custom content state
                // from onResume.
                mMoveToDefaultScreenFromNewIntent = true;
                mWorkspace.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mWorkspace != null) {
                            mWorkspace.moveToDefaultScreen(true);
                        }
                    }
                });
            }
        }

        if (DEBUG_RESUME_TIME) {
            Log.d(TAG, "Time spent in onNewIntent: " + (System.currentTimeMillis() - startTime));
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        for (int page: mSynchronouslyBoundPages) {
            mWorkspace.restoreInstanceStateForChild(page);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mWorkspace.getChildCount() > 0) {
            outState.putInt(RUNTIME_STATE_CURRENT_SCREEN,
                    mWorkspace.getCurrentPageOffsetFromCustomContent());

        }
        super.onSaveInstanceState(outState);

        outState.putInt(RUNTIME_STATE, mState.ordinal());
        // We close any open folders and shortcut containers since they will not be re-opened,
        // and we need to make sure this state is reflected.
        AbstractFloatingView.closeAllOpenViews(this, false);

        if (mPendingRequestArgs != null) {
            outState.putParcelable(RUNTIME_STATE_PENDING_REQUEST_ARGS, mPendingRequestArgs);
        }
        if (mPendingActivityResult != null) {
            outState.putParcelable(RUNTIME_STATE_PENDING_ACTIVITY_RESULT, mPendingActivityResult);
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onSaveInstanceState(outState);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mReceiver);
        unregisterReceiver(mWiFiReceiver);
        //unregisterReceiver(mAlarmReceiver);
        mWorkspace.removeCallbacks(mBuildLayersRunnable);
        mWorkspace.removeFolderListeners();

        // Stop callbacks from LauncherModel
        // It's possible to receive onDestroy after a new Launcher activity has
        // been created. In this case, don't interfere with the new Launcher.
        if (mModel.isCurrentCallbacks(this)) {
            mModel.stopLoader();
            LauncherAppState.getInstance(this).setLauncher(null);
        }

        if (mRotationPrefChangeHandler != null) {
            mSharedPrefs.unregisterOnSharedPreferenceChangeListener(mRotationPrefChangeHandler);
        }

        if(mSchedulePrefChangeHandler != null) {
            mSharedPrefs.unregisterOnSharedPreferenceChangeListener(mSchedulePrefChangeHandler);
        }

        if(mMinimalDesignPrefChangeHandler != null) {
            mSharedPrefs.unregisterOnSharedPreferenceChangeListener(mMinimalDesignPrefChangeHandler);
        }

        if (mSSIDPrefChangeHandler != null) {
            mSharedPrefs.unregisterOnSharedPreferenceChangeListener(mSSIDPrefChangeHandler);
        }
        if (profileChangeHandler != null){
            mSharedPrefs.unregisterOnSharedPreferenceChangeListener(profileChangeHandler);
        }
        if(mLogProfileEditedHandler != null){
            mSharedPrefs.unregisterOnSharedPreferenceChangeListener(mLogProfileEditedHandler);
        }

        try {
            mAppWidgetHost.stopListening();
        } catch (NullPointerException ex) {
            Log.w(TAG, "problem while stopping AppWidgetHost during Launcher destruction", ex);
        }
        mAppWidgetHost = null;

        TextKeyListener.getInstance().release();

        ((AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE))
                .removeAccessibilityStateChangeListener(this);

        WallpaperColorInfo.getInstance(this).setOnThemeChangeListener(null);

        LauncherAnimUtils.onDestroyActivity();

        clearPendingBinds();

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onDestroy();
        }
    }

    public LauncherAccessibilityDelegate getAccessibilityDelegate() {
        return mAccessibilityDelegate;
    }

    public DragController getDragController() {
        return mDragController;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        super.startActivityForResult(intent, requestCode, options);
    }

    @Override
    public void startIntentSenderForResult (IntentSender intent, int requestCode,
            Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) {
        try {
            super.startIntentSenderForResult(intent, requestCode,
                fillInIntent, flagsMask, flagsValues, extraFlags, options);
        } catch (IntentSender.SendIntentException e) {
            throw new ActivityNotFoundException();
        }
    }

    /**
     * Indicates that we want global search for this activity by setting the globalSearch
     * argument for {@link #startSearch} to true.
     */
    @Override
    public void startSearch(String initialQuery, boolean selectInitialQuery,
            Bundle appSearchData, boolean globalSearch) {

        if (initialQuery == null) {
            // Use any text typed in the launcher as the initial query
            initialQuery = getTypedText();
        }
        if (appSearchData == null) {
            appSearchData = new Bundle();
            appSearchData.putString("source", "launcher-search");
        }

        if (mLauncherCallbacks == null ||
                !mLauncherCallbacks.startSearch(initialQuery, selectInitialQuery, appSearchData)) {
            // Starting search from the callbacks failed. Start the default global search.
            startGlobalSearch(initialQuery, selectInitialQuery, appSearchData, null);
        }

        // We need to show the workspace after starting the search
        showWorkspace(true);
    }

    /**
     * Starts the global search activity. This code is a copied from SearchManager
     */
    public void startGlobalSearch(String initialQuery,
            boolean selectInitialQuery, Bundle appSearchData, Rect sourceBounds) {
        final SearchManager searchManager =
            (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        ComponentName globalSearchActivity = searchManager.getGlobalSearchActivity();
        if (globalSearchActivity == null) {
            Log.w(TAG, "No global search activity found.");
            return;
        }
        Intent intent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(globalSearchActivity);
        // Make sure that we have a Bundle to put source in
        if (appSearchData == null) {
            appSearchData = new Bundle();
        } else {
            appSearchData = new Bundle(appSearchData);
        }
        // Set source to package name of app that starts global search if not set already.
        if (!appSearchData.containsKey("source")) {
            appSearchData.putString("source", getPackageName());
        }
        intent.putExtra(SearchManager.APP_DATA, appSearchData);
        if (!TextUtils.isEmpty(initialQuery)) {
            intent.putExtra(SearchManager.QUERY, initialQuery);
        }
        if (selectInitialQuery) {
            intent.putExtra(SearchManager.EXTRA_SELECT_QUERY, selectInitialQuery);
        }
        intent.setSourceBounds(sourceBounds);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Global search activity not found: " + globalSearchActivity);
        }
    }

    public boolean isOnCustomContent() {
        return mWorkspace.isOnOrMovingToCustomContent();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.onPrepareOptionsMenu(menu);
        }
        return false;
    }

    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null, true);
        // Use a custom animation for launching search
        return true;
    }

    public boolean isWorkspaceLocked() {
        return mWorkspaceLoading || mPendingRequestArgs != null;
    }

    public boolean isWorkspaceLoading() {
        return mWorkspaceLoading;
    }

    protected void setWorkspaceLoading(boolean value) {
        boolean isLocked = isWorkspaceLocked();
        mWorkspaceLoading = value;
        if (isLocked != isWorkspaceLocked()) {
            onWorkspaceLockedChanged();
        }
    }

    public void setWaitingForResult(PendingRequestArgs args) {
        boolean isLocked = isWorkspaceLocked();
        mPendingRequestArgs = args;
        if (isLocked != isWorkspaceLocked()) {
            onWorkspaceLockedChanged();
        }
    }

    protected void onWorkspaceLockedChanged() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onWorkspaceLockedChanged();
        }
    }

    void addAppWidgetFromDropImpl(int appWidgetId, ItemInfo info, AppWidgetHostView boundWidget,
            WidgetAddFlowHandler addFlowHandler) {
        if (LOGD) {
            Log.d(TAG, "Adding widget from drop");
        }
        addAppWidgetImpl(appWidgetId, info, boundWidget, addFlowHandler, 0);
    }

    void addAppWidgetImpl(int appWidgetId, ItemInfo info,
            AppWidgetHostView boundWidget, WidgetAddFlowHandler addFlowHandler, int delay) {
        if (!addFlowHandler.startConfigActivity(this, appWidgetId, info, REQUEST_CREATE_APPWIDGET)) {
            // If the configuration flow was not started, add the widget

            Runnable onComplete = new Runnable() {
                @Override
                public void run() {
                    // Exit spring loaded mode if necessary after adding the widget
                    exitSpringLoadedDragModeDelayed(true, EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT,
                            null);
                }
            };
            completeAddAppWidget(appWidgetId, info, boundWidget, addFlowHandler.getProviderInfo(this));
            mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete, delay, false);
        }
    }

    protected void moveToCustomContentScreen(boolean animate) {
        // Close any folders that may be open.
        AbstractFloatingView.closeAllOpenViews(this, animate);
        mWorkspace.moveToCustomContentScreen(animate);
    }

    public void addPendingItem(PendingAddItemInfo info, long container, long screenId,
            int[] cell, int spanX, int spanY) {
        info.container = container;
        info.screenId = screenId;
        if (cell != null) {
            info.cellX = cell[0];
            info.cellY = cell[1];
        }
        info.spanX = spanX;
        info.spanY = spanY;

        switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET:
            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                addAppWidgetFromDrop((PendingAddWidgetInfo) info);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                processShortcutFromDrop((PendingAddShortcutInfo) info);
                break;
            default:
                throw new IllegalStateException("Unknown item type: " + info.itemType);
            }
    }

    /**
     * Process a shortcut drop.
     */
    private void processShortcutFromDrop(PendingAddShortcutInfo info) {
        Intent intent = new Intent(Intent.ACTION_CREATE_SHORTCUT).setComponent(info.componentName);
        setWaitingForResult(PendingRequestArgs.forIntent(REQUEST_CREATE_SHORTCUT, intent, info));
        if (!info.activityInfo.startConfigActivity(this, REQUEST_CREATE_SHORTCUT)) {
            handleActivityResult(REQUEST_CREATE_SHORTCUT, RESULT_CANCELED, null);
        }
    }

    /**
     * Process a widget drop.
     */
    private void addAppWidgetFromDrop(PendingAddWidgetInfo info) {
        AppWidgetHostView hostView = info.boundWidget;
        int appWidgetId;
        WidgetAddFlowHandler addFlowHandler = info.getHandler();
        if (hostView != null) {
            // In the case where we've prebound the widget, we remove it from the DragLayer
            if (LOGD) {
                Log.d(TAG, "Removing widget view from drag layer and setting boundWidget to null");
            }
            getDragLayer().removeView(hostView);

            appWidgetId = hostView.getAppWidgetId();
            addAppWidgetFromDropImpl(appWidgetId, info, hostView, addFlowHandler);

            // Clear the boundWidget so that it doesn't get destroyed.
            info.boundWidget = null;
        } else {
            // In this case, we either need to start an activity to get permission to bind
            // the widget, or we need to start an activity to configure the widget, or both.
            appWidgetId = getAppWidgetHost().allocateAppWidgetId();
            Bundle options = info.bindOptions;

            boolean success = mAppWidgetManager.bindAppWidgetIdIfAllowed(
                    appWidgetId, info.info, options);
            if (success) {
                addAppWidgetFromDropImpl(appWidgetId, info, null, addFlowHandler);
            } else {
                addFlowHandler.startBindFlow(this, appWidgetId, info, REQUEST_BIND_APPWIDGET);
            }
        }
    }

    FolderIcon addFolder(CellLayout layout, long container, final long screenId, int cellX,
            int cellY) {
        final FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = getText(R.string.folder_name);

        // Update the model
        getModelWriter().addItemToDatabase(folderInfo, container, screenId, cellX, cellY);

        // Create the view
        FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon, this, layout, folderInfo);
        mWorkspace.addInScreen(newFolder, folderInfo);
        // Force measure the new folder icon
        CellLayout parent = mWorkspace.getParentCellLayoutForView(newFolder);
        parent.getShortcutsAndWidgets().measureChild(newFolder);
        return newFolder;
    }

    /**
     * Unbinds the view for the specified item, and removes the item and all its children.
     *
     * @param v the view being removed.
     * @param itemInfo the {@link ItemInfo} for this view.
     * @param deleteFromDb whether or not to delete this item from the db.
     */
    public boolean removeItem(View v, final ItemInfo itemInfo, boolean deleteFromDb) {
        if (itemInfo instanceof ShortcutInfo) {
            // Remove the shortcut from the folder before removing it from launcher
            View folderIcon = mWorkspace.getHomescreenIconByItemId(itemInfo.container);
            if (folderIcon instanceof FolderIcon) {
                ((FolderInfo) folderIcon.getTag()).remove((ShortcutInfo) itemInfo, true);
            } else {
                mWorkspace.removeWorkspaceItem(v);
                //itemInfo.title returns the title of the remove app shortcut
                Set<String> appsOnHomescreen = mSharedPrefs.getStringSet(APPS_ON_HOMESCREEN, null);
                String currentProfile = mSharedPrefs.getString(CURRENT_PROFILE_PREF, null);
                if(currentProfile!=null && appsOnHomescreen!=null){
                    if(currentProfile.equals("home")||currentProfile.equals("disconnected")||currentProfile.equals("work")||currentProfile.equals("default")){
                        //do nothing
                    } else {
                        for(String newAddedProfile : newAddedProfiles){
                            if(newAddedProfile.substring(1).equals(currentProfile)){
                                currentProfile = newAddedProfile.charAt(0)+"";
                            }
                        }
                    }
                    ArrayList<String> appsOnHomescreenList = new ArrayList<>(appsOnHomescreen);
                    for (String profileApps : appsOnHomescreen) {
                        String profileName = profileApps.split("_")[0];
                        if(profileName.equals(currentProfile)){
                            if(profileApps.split("_").length>1){
                              List<String> appsFromProfile = Arrays.asList(profileApps.split("_")[1].split(","));
                                String updatedAppsForProfile = profileName+"_";
                                appsOnHomescreenList.remove(profileApps);
                                boolean firstTime = true;
                                //add all apps except the removed one
                                for(String app : appsFromProfile){
                                    if(!app.equals(itemInfo.title.toString())){
                                        if(firstTime){
                                            updatedAppsForProfile = updatedAppsForProfile+app;
                                            firstTime = false;
                                        } else {
                                            updatedAppsForProfile = updatedAppsForProfile+","+app;
                                        }
                                    }
                                }
                                appsOnHomescreenList.add(updatedAppsForProfile);
                                Set set = new HashSet(appsOnHomescreenList);
                                mSharedPrefs.edit().putStringSet(APPS_ON_HOMESCREEN, set).apply();
                                fetchHomescreenAppList();
                            }
                        }
                    }
                }
                if(currentProfile.equals("home")||currentProfile.equals("work")||currentProfile.equals("disconnected")||currentProfile.equals("default")){
                    LogEntryProfileEdited logEntry = new LogEntryProfileEdited(currentProfile, "app removed from homescreen", getSSIDPref(currentProfile), getSchedulePref(currentProfile), getRingtonePref(currentProfile), getNotificationSoundPref(currentProfile), getNotificationBlockedPref(currentProfile), getMinimalDesignPref(currentProfile), getHomeScreenAppsList(currentProfile), getWallpaperInfo(currentProfile), getGrayScalePref(currentProfile));
                    firebaseLogger.addLogMessage("events", "profile edited", logEntry);
                } else {
                    Set set = mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
                    if(set!=null){
                        ArrayList<String> newAddedProfiles = new ArrayList<>(set);
                        for(String newAddedProfile : newAddedProfiles){
                            if((newAddedProfile.charAt(0)+"").equals(currentProfile)){
                                String profile = newAddedProfile.charAt(0)+"";
                                LogEntryProfileEdited logEntry = new LogEntryProfileEdited(newAddedProfile.substring(1), "app removed from homescreen", getSSIDPref(profile), getSchedulePref(profile), getRingtonePref(profile), getNotificationSoundPref(profile), getNotificationBlockedPref(profile), getMinimalDesignPref(profile), getHomeScreenAppsList(profile), getWallpaperInfo(profile), getGrayScalePref(profile));
                                firebaseLogger.addLogMessage("events", "profile edited", logEntry);
                            }
                        }
                    }
                }
            }
            if (deleteFromDb) {
                getModelWriter().deleteItemFromDatabase(itemInfo);
            }
        } else if (itemInfo instanceof FolderInfo) {
            final FolderInfo folderInfo = (FolderInfo) itemInfo;
            if (v instanceof FolderIcon) {
                ((FolderIcon) v).removeListeners();
            }
            mWorkspace.removeWorkspaceItem(v);
            if (deleteFromDb) {
                getModelWriter().deleteFolderAndContentsFromDatabase(folderInfo);
            }
        } else if (itemInfo instanceof LauncherAppWidgetInfo) {
            final LauncherAppWidgetInfo widgetInfo = (LauncherAppWidgetInfo) itemInfo;
            mWorkspace.removeWorkspaceItem(v);
            if (deleteFromDb) {
                deleteWidgetInfo(widgetInfo);
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Deletes the widget info and the widget id.
     */
    private void deleteWidgetInfo(final LauncherAppWidgetInfo widgetInfo) {
        final LauncherAppWidgetHost appWidgetHost = getAppWidgetHost();
        if (appWidgetHost != null && !widgetInfo.isCustomWidget() && widgetInfo.isWidgetIdAllocated()) {
            // Deleting an app widget ID is a void call but writes to disk before returning
            // to the caller...
            new AsyncTask<Void, Void, Void>() {
                public Void doInBackground(Void ... args) {
                    appWidgetHost.deleteAppWidgetId(widgetInfo.appWidgetId);
                    return null;
                }
            }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR);
        }
        getModelWriter().deleteItemFromDatabase(widgetInfo);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return (event.getKeyCode() == KeyEvent.KEYCODE_HOME) || super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (mLauncherCallbacks != null && mLauncherCallbacks.handleBackPressed()) {
            return;
        }

        if (mDragController.isDragging()) {
            mDragController.cancelDrag();
            return;
        }

        // Note: There should be at most one log per method call. This is enforced implicitly
        // by using if-else statements.
        UserEventDispatcher ued = getUserEventDispatcher();
        AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(this);
        if (topView != null) {
            if (topView.getActiveTextView() != null) {
                topView.getActiveTextView().dispatchBackKey();
            } else {
                if (topView instanceof PopupContainerWithArrow) {
                    ued.logActionCommand(Action.Command.BACK,
                            topView.getExtendedTouchView(), ContainerType.DEEPSHORTCUTS);
                } else if (topView instanceof Folder) {
                    ued.logActionCommand(Action.Command.BACK,
                            ((Folder) topView).getFolderIcon(), ContainerType.FOLDER);
                }
                topView.close(true);
            }
        } else if (isAppsViewVisible()) {
            ued.logActionCommand(Action.Command.BACK, ContainerType.ALLAPPS);
            showWorkspace(true);
        } else if (isWidgetsViewVisible())  {
            ued.logActionCommand(Action.Command.BACK, ContainerType.WIDGETS);
            showOverviewMode(true);
        } else if (mWorkspace.isInOverviewMode()) {
            ued.logActionCommand(Action.Command.BACK, ContainerType.OVERVIEW);
            showWorkspace(true);
        } else {
            // TODO: Log this case.
            mWorkspace.exitWidgetResizeMode();
        }
        String currentProfile = mSharedPrefs.getString(CURRENT_PROFILE_PREF, "default");
        if(currentProfile.equals("home") || currentProfile.equals("work") || currentProfile.equals("default") ||currentProfile.equals("disconnected") ){
            isMinimalDesignON = mSharedPrefs.getBoolean(currentProfile + ProfilesActivity.MINIMAL_DESIGN_PREF, false);
            if(isMinimalDesignON){
                allAppsListView.setVisibility(View.INVISIBLE);
                launcherListView.setVisibility(View.VISIBLE);
            }
        } else {
            Set set = mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
            if(set!=null){
                newAddedProfiles = new ArrayList<String>(set);
                if(newAddedProfiles!=null){
                    for(String sub : newAddedProfiles){
                        if(sub.substring(1).equals(currentProfile)){
                            String profileID = sub.charAt(0)+"";
                            isMinimalDesignON = mSharedPrefs.getBoolean(profileID + ProfilesActivity.MINIMAL_DESIGN_PREF, false);
                            if(isMinimalDesignON){
                                allAppsListView.setVisibility(View.INVISIBLE);
                                launcherListView.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }
            }
        }
    }

    public void saveWallpaperInfo(){
        Set wallpaperSet = mSharedPrefs.getStringSet(WALLPAPER_INFO, null);
        ArrayList<String> wallpaperInfos;
        if(wallpaperSet==null){
            wallpaperInfos = new ArrayList<>();
        } else {
            wallpaperInfos = new ArrayList<>(wallpaperSet);
        }
        for(String profile : availableProfiles){
            String profileToReplace = "";
            boolean replaceID = false;
            if(profile.equals("work")||profile.equals("home")||profile.equals("disconnected")||profile.equals("default")){
                for(String profileWithWallpaper : wallpaperInfos){
                    if(profileWithWallpaper.split("_")[0].equals(profile)){
                        profileToReplace = profileWithWallpaper;
                        replaceID = true;
                    }
                }
                if(replaceID){
                    wallpaperInfos.remove(profileToReplace);
                }
                String newWallpaperInfo = profile+"_"+UUID.randomUUID().toString().substring(0,7);
                wallpaperInfos.add(newWallpaperInfo);
                mSharedPrefs.edit().putStringSet(WALLPAPER_INFO, new HashSet<String>(wallpaperInfos)).apply();
            } else {
                Set set = mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
                if(set!=null){
                    ArrayList<String> addedProfiles = new ArrayList<>(set);
                    for(String addedProfile : addedProfiles){
                        if(addedProfile.substring(1).equals(profile)){
                            String profileID = addedProfile.charAt(0)+"";
                            for(String profileWithWallpaper : wallpaperInfos){
                                if(profileWithWallpaper.split("_")[0].equals(profileID)){
                                    profileToReplace = profileID;
                                    replaceID = true;
                                }
                            }
                            if(replaceID){
                                wallpaperInfos.remove(profileToReplace);
                            }
                            String newWallpaperInfo = profileID+"_"+UUID.randomUUID().toString().substring(0,7);
                            wallpaperInfos.add(newWallpaperInfo);
                            mSharedPrefs.edit().putStringSet(WALLPAPER_INFO, new HashSet<String>(wallpaperInfos)).apply();
                        }
                    }
                }
            }
        }
    }

    public static String getWallpaperInfo(String profile){
        Set set = mSharedPrefs.getStringSet(WALLPAPER_INFO, null);
        if(set!=null){
            ArrayList<String> wallpaperInfos = new ArrayList<>(set);
            for (String wallpaperInfo : wallpaperInfos){
                if(wallpaperInfo.split("_")[0].equals(profile)){
                    return wallpaperInfo.split("_")[1];
                }
            }
        }
        return "empty";
    }

    /**
     * Launches the intent referred by the clicked shortcut.
     *
     * @param v The view representing the clicked shortcut.
     */
    public void onClick(View v) {
        // Make sure that rogue clicks don't get through while allapps is launching, or after the
        // view has detached (it's possible for this to happen if the view is removed mid touch).
        if (v.getWindowToken() == null) {
            return;
        }

        if (!mWorkspace.isFinishedSwitchingState()) {
            return;
        }

        String currentProfile = mSharedPrefs.getString(CURRENT_PROFILE_PREF, null);
        if(v.getTag() instanceof ShortcutInfo){
            String shortcutName = ((ShortcutInfo) v.getTag()).title+"_"+currentProfile;
            usedShortcuts.add(shortcutName); //for firebase logging
        }
        if(v.getTag() instanceof AppInfo){
            String appName = ((AppInfo) v.getTag()).title+"_"+currentProfile;
            usedApps.add(appName); //for firebase logging
        }

        if (v instanceof Workspace) {
            if (mWorkspace.isInOverviewMode()) {
                getUserEventDispatcher().logActionOnContainer(LauncherLogProto.Action.Type.TOUCH,
                        LauncherLogProto.Action.Direction.NONE,
                        LauncherLogProto.ContainerType.OVERVIEW, mWorkspace.getCurrentPage());
                showWorkspace(true);
            }
            return;
        }

        if (v instanceof CellLayout) {
            if (mWorkspace.isInOverviewMode()) {
                int page = mWorkspace.indexOfChild(v);
                getUserEventDispatcher().logActionOnContainer(LauncherLogProto.Action.Type.TOUCH,
                        LauncherLogProto.Action.Direction.NONE,
                        LauncherLogProto.ContainerType.OVERVIEW, page);
                mWorkspace.snapToPageFromOverView(page);
                showWorkspace(true);
            }
            return;
        }

        Object tag = v.getTag();
        if (tag instanceof ShortcutInfo) {
            onClickAppShortcut(v);
        } else if (tag instanceof FolderInfo) {
            if (v instanceof FolderIcon) {
                onClickFolderIcon(v);
            }
        } else if ((v instanceof PageIndicator) ||
            (v == mAllAppsButton && mAllAppsButton != null)) {
            onClickAllAppsButton(v);
        } else if (tag instanceof AppInfo) {
            startAppShortcutOrInfoActivity(v);
        } else if (tag instanceof LauncherAppWidgetInfo) {
            if (v instanceof PendingAppWidgetHostView) {
                onClickPendingWidget((PendingAppWidgetHostView) v);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    /**
     * Event handler for the app widget view which has not fully restored.
     */
    public void onClickPendingWidget(final PendingAppWidgetHostView v) {
        if (mIsSafeModeEnabled) {
            Toast.makeText(this, R.string.safemode_widget_error, Toast.LENGTH_SHORT).show();
            return;
        }

        final LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) v.getTag();
        if (v.isReadyForClickSetup()) {
            LauncherAppWidgetProviderInfo appWidgetInfo =
                    mAppWidgetManager.findProvider(info.providerName, info.user);
            if (appWidgetInfo == null) {
                return;
            }
            WidgetAddFlowHandler addFlowHandler = new WidgetAddFlowHandler(appWidgetInfo);

            if (info.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_NOT_VALID)) {
                if (!info.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_ALLOCATED)) {
                    // This should not happen, as we make sure that an Id is allocated during bind.
                    return;
                }
                addFlowHandler.startBindFlow(this, info.appWidgetId, info,
                        REQUEST_BIND_PENDING_APPWIDGET);
            } else {
                addFlowHandler.startConfigActivity(this, info, REQUEST_RECONFIGURE_APPWIDGET);
            }
        } else {
            final String packageName = info.providerName.getPackageName();
            onClickPendingAppItem(v, packageName, info.installProgress >= 0);
        }
    }

    /**
     * Event handler for the "grid" button or "caret" that appears on the home screen, which
     * enters all apps mode. In verticalBarLayout the caret can be seen when all apps is open, and
     * so in that case reverses the action.
     *
     * @param v The view that was clicked.
     */
    protected void onClickAllAppsButton(View v) {
        if (LOGD) Log.d(TAG, "onClickAllAppsButton");
        if (!isAppsViewVisible()) {
            getUserEventDispatcher().logActionOnControl(Action.Touch.TAP,
                    ControlType.ALL_APPS_BUTTON);
            showAppsView(true /* animated */, true /* updatePredictedApps */,
                    false /* focusSearchBar */);
        } else {
            showWorkspace(true);
        }
    }

    protected void onLongClickAllAppsButton(View v) {
        if (LOGD) Log.d(TAG, "onLongClickAllAppsButton");
        if (!isAppsViewVisible()) {
            getUserEventDispatcher().logActionOnControl(Action.Touch.LONGPRESS,
                    ControlType.ALL_APPS_BUTTON);
            showAppsView(true /* animated */,
                    true /* updatePredictedApps */, true /* focusSearchBar */);
        } else {
            showWorkspace(true);
        }
    }

    private void onClickPendingAppItem(final View v, final String packageName,
            boolean downloadStarted) {
        if (downloadStarted) {
            // If the download has started, simply direct to the market app.
            startMarketIntentForPackage(v, packageName);
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle(R.string.abandoned_promises_title)
            .setMessage(R.string.abandoned_promise_explanation)
            .setPositiveButton(R.string.abandoned_search, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startMarketIntentForPackage(v, packageName);
                }
            })
            .setNeutralButton(R.string.abandoned_clean_this,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final UserHandle user = Process.myUserHandle();
                        mWorkspace.removeAbandonedPromise(packageName, user);
                    }
                })
            .create().show();
    }

    private void startMarketIntentForPackage(View v, String packageName) {
        ItemInfo item = (ItemInfo) v.getTag();
        Intent intent = PackageManagerHelper.getMarketIntent(packageName);
        boolean success = startActivitySafely(v, intent, item);
        if (success && v instanceof BubbleTextView) {
            mWaitingForResume = (BubbleTextView) v;
            mWaitingForResume.setStayPressed(true);
        }
    }

    /**
     * Event handler for an app shortcut click.
     *
     * @param v The view that was clicked. Must be a tagged with a {@link ShortcutInfo}.
     */
    protected void onClickAppShortcut(final View v) {
        if (LOGD) Log.d(TAG, "onClickAppShortcut");
        Object tag = v.getTag();
        if (!(tag instanceof ShortcutInfo)) {
            throw new IllegalArgumentException("Input must be a Shortcut");
        }



        // Open shortcut
        final ShortcutInfo shortcut = (ShortcutInfo) tag;

        if (shortcut.isDisabled != 0) {
            if ((shortcut.isDisabled &
                    ~ShortcutInfo.FLAG_DISABLED_SUSPENDED &
                    ~ShortcutInfo.FLAG_DISABLED_QUIET_USER) == 0) {
                // If the app is only disabled because of the above flags, launch activity anyway.
                // Framework will tell the user why the app is suspended.
            } else {
                if (!TextUtils.isEmpty(shortcut.disabledMessage)) {
                    // Use a message specific to this shortcut, if it has one.
                    Toast.makeText(this, shortcut.disabledMessage, Toast.LENGTH_SHORT).show();
                    return;
                }
                // Otherwise just use a generic error message.
                int error = R.string.activity_not_available;
                if ((shortcut.isDisabled & ShortcutInfo.FLAG_DISABLED_SAFEMODE) != 0) {
                    error = R.string.safemode_shortcut_error;
                } else if ((shortcut.isDisabled & ShortcutInfo.FLAG_DISABLED_BY_PUBLISHER) != 0 ||
                        (shortcut.isDisabled & ShortcutInfo.FLAG_DISABLED_LOCKED_USER) != 0) {
                    error = R.string.shortcut_not_available;
                }
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Check for abandoned promise
        if ((v instanceof BubbleTextView) && shortcut.hasPromiseIconUi()) {
            String packageName = shortcut.intent.getComponent() != null ?
                    shortcut.intent.getComponent().getPackageName() : shortcut.intent.getPackage();
            if (!TextUtils.isEmpty(packageName)) {
                onClickPendingAppItem(v, packageName,
                        shortcut.hasStatusFlag(ShortcutInfo.FLAG_INSTALL_SESSION_ACTIVE));
                return;
            }
        }

        // Start activities
        startAppShortcutOrInfoActivity(v);
    }

    private void startAppShortcutOrInfoActivity(View v) {
        ItemInfo item = (ItemInfo) v.getTag();
        Intent intent;
        if (item instanceof PromiseAppInfo) {
            PromiseAppInfo promiseAppInfo = (PromiseAppInfo) item;
            intent = promiseAppInfo.getMarketIntent();
        } else {
            intent = item.getIntent();
        }
        if (intent == null) {
            throw new IllegalArgumentException("Input must have a valid intent");
        }
        boolean success = startActivitySafely(v, intent, item);
        getUserEventDispatcher().logAppLaunch(v, intent, item.user); // TODO for discovered apps b/35802115

        if (success && v instanceof BubbleTextView) {
            mWaitingForResume = (BubbleTextView) v;
            mWaitingForResume.setStayPressed(true);
        }
    }

    /**
     * Event handler for a folder icon click.
     *
     * @param v The view that was clicked. Must be an instance of {@link FolderIcon}.
     */
    protected void onClickFolderIcon(View v) {
        if (LOGD) Log.d(TAG, "onClickFolder");
        if (!(v instanceof FolderIcon)){
            throw new IllegalArgumentException("Input must be a FolderIcon");
        }

        Folder folder = ((FolderIcon) v).getFolder();
        if (!folder.isOpen() && !folder.isDestroyed()) {
            // Open the requested folder
            folder.animateOpen();
        }
    }

    /*
     * Event handler for the (Add) Widgets button that appears after a long press
     * on the home screen.
     *
    public void onClickAddWidgetButton(View view) {
        if (LOGD) Log.d(TAG, "onClickAddWidgetButton");
        if (mIsSafeModeEnabled) {
            Toast.makeText(this, R.string.safemode_widget_error, Toast.LENGTH_SHORT).show();
        } else {
            showWidgetsView(true, true);
        }
    }
    */

    /**
     * Event handler for the wallpaper picker button that appears after a long press
     * on the home screen.
     */
    public void onClickWallpaperPicker(View v) {
        if (!Utilities.isWallpaperAllowed(this)) {
            Toast.makeText(this, R.string.msg_disabled_by_admin, Toast.LENGTH_SHORT).show();
            return;
        }

        int pageScroll = mWorkspace.getScrollForPage(mWorkspace.getPageNearestToCenterOfScreen());
        float offset = mWorkspace.mWallpaperOffset.wallpaperOffsetForScroll(pageScroll);
        setWaitingForResult(new PendingRequestArgs(new ItemInfo()));
        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER)
                .putExtra(Utilities.EXTRA_WALLPAPER_OFFSET, offset);

        String pickerPackage = getString(R.string.wallpaper_picker_package);
        boolean hasTargetPackage = !TextUtils.isEmpty(pickerPackage);
        try {
            if (hasTargetPackage && getPackageManager().getApplicationInfo(pickerPackage, 0).enabled) {
                intent.setPackage(pickerPackage);
            }
        } catch (PackageManager.NameNotFoundException ex) {
        }

        intent.setSourceBounds(getViewBounds(v));
        try {
            startActivityForResult(intent, REQUEST_PICK_WALLPAPER,
                    // If there is no target package, use the default intent chooser animation
                    hasTargetPackage ? getActivityLaunchOptions(v) : null);
        } catch (ActivityNotFoundException e) {
            setWaitingForResult(null);
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Event handler for a click on the settings button that appears after a long press
     * on the home screen.
     */
    public void onClickSettingsButton(View v) {
        if (LOGD) Log.d(TAG, "onClickSettingsButton");
        Intent intent = new Intent(Intent.ACTION_APPLICATION_PREFERENCES);
        //        .setPackage(getPackageName());
        //intent.setSourceBounds(getViewBounds(v));
        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //startActivity(intent, getActivityLaunchOptions(v));
        startActivity(intent);
    }

    /**
     * Event handler for a click on the profiles button that appears after a long press
     * on the home screen.
     */
    public void onClickProfilesButton(View v) {
        if (LOGD) Log.d(TAG, "onClickProfilesButton");
        Intent intent = new Intent(getApplicationContext(), ProfilesActivity.class);
        //        .setPackage(getPackageName());
        //intent.setSourceBounds(getViewBounds(v));
        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //startActivity(intent, getActivityLaunchOptions(v));
        startActivity(intent);
    }

    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
        mDragLayer.onAccessibilityStateChanged(enabled);
    }

    public void onDragStarted() {
        if (isOnCustomContent()) {
            // Custom content screen doesn't participate in drag and drop. If on custom
            // content screen, move to default.
            moveWorkspaceToDefaultScreen();
        }
    }

    /**
     * Called when the user stops interacting with the launcher.
     * This implies that the user is now on the homescreen and is not doing housekeeping.
     */
    protected void onInteractionEnd() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onInteractionEnd();
        }
    }

    /**
     * Called when the user starts interacting with the launcher.
     * The possible interactions are:
     *  - open all apps
     *  - reorder an app shortcut, or a widget
     *  - open the overview mode.
     * This is a good time to stop doing things that only make sense
     * when the user is on the homescreen and not doing housekeeping.
     */
    protected void onInteractionBegin() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onInteractionBegin();
        }
    }

    /** Updates the interaction state. */
    public void updateInteraction(Workspace.State fromState, Workspace.State toState) {
        // Only update the interacting state if we are transitioning to/from a view with an
        // overlay
        boolean fromStateWithOverlay = fromState != Workspace.State.NORMAL;
        boolean toStateWithOverlay = toState != Workspace.State.NORMAL;
        if (toStateWithOverlay) {
            onInteractionBegin();
        } else if (fromStateWithOverlay) {
            onInteractionEnd();
        }
    }

    private void startShortcutIntentSafely(Intent intent, Bundle optsBundle, ItemInfo info) {
        try {
            StrictMode.VmPolicy oldPolicy = StrictMode.getVmPolicy();
            try {
                // Temporarily disable deathPenalty on all default checks. For eg, shortcuts
                // containing file Uri's would cause a crash as penaltyDeathOnFileUriExposure
                // is enabled by default on NYC.
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll()
                        .penaltyLog().build());

                if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT) {
                    String id = ((ShortcutInfo) info).getDeepShortcutId();
                    String packageName = intent.getPackage();
                    DeepShortcutManager.getInstance(this).startShortcut(
                            packageName, id, intent, optsBundle, info.user);
                } else {
                    // Could be launching some bookkeeping activity
                    startActivity(intent, optsBundle);
                }
            } finally {
                StrictMode.setVmPolicy(oldPolicy);
            }
        } catch (SecurityException e) {
            // Due to legacy reasons, direct call shortcuts require Launchers to have the
            // corresponding permission. Show the appropriate permission prompt if that
            // is the case.
            if (intent.getComponent() == null
                    && Intent.ACTION_CALL.equals(intent.getAction())
                    && checkSelfPermission(Manifest.permission.CALL_PHONE) !=
                    PackageManager.PERMISSION_GRANTED) {

                setWaitingForResult(PendingRequestArgs
                        .forIntent(REQUEST_PERMISSION_CALL_PHONE, intent, info));
                requestPermissions(new String[]{Manifest.permission.CALL_PHONE},
                        REQUEST_PERMISSION_CALL_PHONE);
            } else {
                // No idea why this was thrown.
                throw e;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public Bundle getActivityLaunchOptions(View v) {
        if (Utilities.ATLEAST_MARSHMALLOW) {
            int left = 0, top = 0;
            int width = v.getMeasuredWidth(), height = v.getMeasuredHeight();
            if (v instanceof BubbleTextView) {
                // Launch from center of icon, not entire view
                Drawable icon = ((BubbleTextView) v).getIcon();
                if (icon != null) {
                    Rect bounds = icon.getBounds();
                    left = (width - bounds.width()) / 2;
                    top = v.getPaddingTop();
                    width = bounds.width();
                    height = bounds.height();
                }
            }
            return ActivityOptions.makeClipRevealAnimation(v, left, top, width, height).toBundle();
        } else if (Utilities.ATLEAST_LOLLIPOP_MR1) {
            // On L devices, we use the device default slide-up transition.
            // On L MR1 devices, we use a custom version of the slide-up transition which
            // doesn't have the delay present in the device default.
            return ActivityOptions.makeCustomAnimation(
                    this, R.anim.task_open_enter, R.anim.no_anim).toBundle();
        }
        return null;
    }

    public Rect getViewBounds(View v) {
        int[] pos = new int[2];
        v.getLocationOnScreen(pos);
        return new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight());
    }

    public boolean startActivitySafely(View v, Intent intent, ItemInfo item) {
        if (mIsSafeModeEnabled && !Utilities.isSystemApp(this, intent)) {
            Toast.makeText(this, R.string.safemode_shortcut_error, Toast.LENGTH_SHORT).show();
            return false;
        }
        // Only launch using the new animation if the shortcut has not opted out (this is a
        // private contract between launcher and may be ignored in the future).
        boolean useLaunchAnimation = (v != null) &&
                !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);
        Bundle optsBundle = useLaunchAnimation ? getActivityLaunchOptions(v) : null;

        UserHandle user = item == null ? null : item.user;

        // Prepare intent
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (v != null) {
            intent.setSourceBounds(getViewBounds(v));
        }
        try {
            if (Utilities.ATLEAST_MARSHMALLOW
                    && (item instanceof ShortcutInfo)
                    && (item.itemType == Favorites.ITEM_TYPE_SHORTCUT
                     || item.itemType == Favorites.ITEM_TYPE_DEEP_SHORTCUT)
                    && !((ShortcutInfo) item).isPromise()) {
                // Shortcuts need some special checks due to legacy reasons.
                startShortcutIntentSafely(intent, optsBundle, item);
            } else if (user == null || user.equals(Process.myUserHandle())) {
                // Could be launching some bookkeeping activity
                startActivity(intent, optsBundle);
            } else {
                LauncherAppsCompat.getInstance(this).startActivityForProfile(
                        intent.getComponent(), user, intent.getSourceBounds(), optsBundle);
            }
            return true;
        } catch (ActivityNotFoundException|SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to launch. tag=" + item + " intent=" + intent, e);
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mLastDispatchTouchEventX = ev.getX();
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onLongClick(View v) {
        if (!isDraggingEnabled()) return false;
        if (isWorkspaceLocked()) return false;
        if (mState != State.WORKSPACE) return false;

        if ((FeatureFlags.NO_ALL_APPS_ICON && v instanceof PageIndicatorCaretLandscape) ||
                (v == mAllAppsButton && mAllAppsButton != null)) {
            onLongClickAllAppsButton(v);
            return true;
        }

        boolean ignoreLongPressToOverview =
                mDeviceProfile.shouldIgnoreLongPressToOverview(mLastDispatchTouchEventX);

        if (v instanceof Workspace) {
            if (!mWorkspace.isInOverviewMode()) {
                if (!mWorkspace.isTouchActive() && !ignoreLongPressToOverview) {
                    getUserEventDispatcher().logActionOnContainer(Action.Touch.LONGPRESS,
                            Action.Direction.NONE, ContainerType.WORKSPACE,
                            mWorkspace.getCurrentPage());
                    showOverviewMode(true);
                    mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        CellLayout.CellInfo longClickCellInfo = null;
        View itemUnderLongClick = null;
        if (v.getTag() instanceof ItemInfo) {
            ItemInfo info = (ItemInfo) v.getTag();
            longClickCellInfo = new CellLayout.CellInfo(v, info);
            itemUnderLongClick = longClickCellInfo.cell;
            mPendingRequestArgs = null;
        }

        // The hotseat touch handling does not go through Workspace, and we always allow long press
        // on hotseat items.
        if (!mDragController.isDragging()) {
            if (itemUnderLongClick == null) {
                // User long pressed on empty space
                if (mWorkspace.isInOverviewMode()) {
                    mWorkspace.startReordering(v);
                    getUserEventDispatcher().logActionOnContainer(Action.Touch.LONGPRESS,
                            Action.Direction.NONE, ContainerType.OVERVIEW);
                } else {
                    if (ignoreLongPressToOverview) {
                        return false;
                    }
                    getUserEventDispatcher().logActionOnContainer(Action.Touch.LONGPRESS,
                            Action.Direction.NONE, ContainerType.WORKSPACE,
                            mWorkspace.getCurrentPage());
                    showOverviewMode(true);
                }
                mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
            } else {
                final boolean isAllAppsButton =
                        !FeatureFlags.NO_ALL_APPS_ICON && isHotseatLayout(v) &&
                                mDeviceProfile.inv.isAllAppsButtonRank(mHotseat.getOrderInHotseat(
                                        longClickCellInfo.cellX, longClickCellInfo.cellY));
                if (!(itemUnderLongClick instanceof Folder || isAllAppsButton)) {
                    // User long pressed on an item
                    mWorkspace.startDrag(longClickCellInfo, new DragOptions());
                }
            }
        }
        return true;
    }

    boolean isHotseatLayout(View layout) {
        // TODO: Remove this method
        return mHotseat != null && layout != null &&
                (layout instanceof CellLayout) && (layout == mHotseat.getLayout());
    }

    /**
     * Returns the CellLayout of the specified container at the specified screen.
     */
    public CellLayout getCellLayout(long container, long screenId) {
        if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            if (mHotseat != null) {
                return mHotseat.getLayout();
            } else {
                return null;
            }
        } else {
            return mWorkspace.getScreenWithId(screenId);
        }
    }

    /**
     * For overridden classes.
     */
    public boolean isAllAppsVisible() {
        return isAppsViewVisible();
    }

    public boolean isAppsViewVisible() {
        return (mState == State.APPS) || (mOnResumeState == State.APPS);
    }

    public boolean isWidgetsViewVisible() {
        return (mState == State.WIDGETS) || (mOnResumeState == State.WIDGETS);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // The widget preview db can result in holding onto over
            // 3MB of memory for caching which isn't necessary.
            SQLiteDatabase.releaseMemory();

            // This clears all widget bitmaps from the widget tray
            // TODO(hyunyoungs)
        }
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onTrimMemory(level);
        }
    }

    public boolean showWorkspace(boolean animated) {
        return showWorkspace(animated, null);
    }

    public boolean showWorkspace(boolean animated, Runnable onCompleteRunnable) {
        boolean changed = mState != State.WORKSPACE ||
                mWorkspace.getState() != Workspace.State.NORMAL;
        if (changed || mAllAppsController.isTransitioning()) {
            mWorkspace.setVisibility(View.VISIBLE);
            mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
                    Workspace.State.NORMAL, animated, onCompleteRunnable);

            // Set focus to the AppsCustomize button
            if (mAllAppsButton != null) {
                mAllAppsButton.requestFocus();
            }
        }

        // Change the state *after* we've called all the transition code
        setState(State.WORKSPACE);

        if (changed) {
            // Send an accessibility event to announce the context change
            getWindow().getDecorView()
                    .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
        return changed;
    }

    /**
     * Shows the overview button.
     */
    public void showOverviewMode(boolean animated) {
        showOverviewMode(animated, false);
    }

    /**
     * Shows the overview button, and if {@param requestButtonFocus} is set, will force the focus
     * onto one of the overview panel buttons.
     */
    void showOverviewMode(boolean animated, boolean requestButtonFocus) {
        Runnable postAnimRunnable = null;
        if (requestButtonFocus) {
            postAnimRunnable = new Runnable() {
                @Override
                public void run() {
                    // Hitting the menu button when in touch mode does not trigger touch mode to
                    // be disabled, so if requested, force focus on one of the overview panel
                    // buttons.
                    mOverviewPanel.requestFocusFromTouch();
                }
            };
        }
        mWorkspace.setVisibility(View.VISIBLE);
        mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
                Workspace.State.OVERVIEW, animated, postAnimRunnable);
        setState(State.WORKSPACE);

        // If animated from long press, then don't allow any of the controller in the drag
        // layer to intercept any remaining touch.
        mWorkspace.requestDisallowInterceptTouchEvent(animated);
    }

    private void setState(State state) {
        this.mState = state;
        updateSoftInputMode();
    }

    private void updateSoftInputMode() {
        if (FeatureFlags.LAUNCHER3_UPDATE_SOFT_INPUT_MODE) {
            final int mode;
            if (isAppsViewVisible()) {
                mode = SOFT_INPUT_MODE_ALL_APPS;
            } else {
                mode = SOFT_INPUT_MODE_DEFAULT;
            }
            getWindow().setSoftInputMode(mode);
        }
    }

    /**
     * Shows the apps view.
     */
    public void showAppsView(boolean animated, boolean updatePredictedApps,
            boolean focusSearchBar) {
        markAppsViewShown();
        if (updatePredictedApps) {
            tryAndUpdatePredictedApps();
        }
        showAppsOrWidgets(State.APPS, animated, focusSearchBar);
    }

    /**
     * Shows the widgets view.
     */
    void showWidgetsView(boolean animated, boolean resetPageToZero) {
        if (LOGD) Log.d(TAG, "showWidgetsView:" + animated + " resetPageToZero:" + resetPageToZero);
        if (resetPageToZero) {
            mWidgetsView.scrollToTop();
        }
        showAppsOrWidgets(State.WIDGETS, animated, false);

        mWidgetsView.post(new Runnable() {
            @Override
            public void run() {
                mWidgetsView.requestFocus();
            }
        });
    }

    /**
     * Sets up the transition to show the apps/widgets view.
     *
     * @return whether the current from and to state allowed this operation
     */
    // TODO: calling method should use the return value so that when {@code false} is returned
    // the workspace transition doesn't fall into invalid state.
    private boolean showAppsOrWidgets(State toState, boolean animated, boolean focusSearchBar) {
        if (!(mState == State.WORKSPACE ||
                mState == State.APPS_SPRING_LOADED ||
                mState == State.WIDGETS_SPRING_LOADED ||
                (mState == State.APPS && mAllAppsController.isTransitioning()))) {
            return false;
        }
        if (toState != State.APPS && toState != State.WIDGETS) {
            return false;
        }

        // This is a safe and supported transition to bypass spring_loaded mode.
        if (mExitSpringLoadedModeRunnable != null) {
            mHandler.removeCallbacks(mExitSpringLoadedModeRunnable);
            mExitSpringLoadedModeRunnable = null;
        }

        if (toState == State.APPS) {
            mStateTransitionAnimation.startAnimationToAllApps(animated, focusSearchBar);
        } else {
            mStateTransitionAnimation.startAnimationToWidgets(animated);
        }

        // Change the state *after* we've called all the transition code
        setState(toState);
        AbstractFloatingView.closeAllOpenViews(this);

        // Send an accessibility event to announce the context change
        getWindow().getDecorView()
                .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        return true;
    }

    /**
     * Updates the workspace and interaction state on state change, and return the animation to this
     * new state.
     */
    public Animator startWorkspaceStateChangeAnimation(Workspace.State toState,
            boolean animated, AnimationLayerSet layerViews) {
        Workspace.State fromState = mWorkspace.getState();
        Animator anim = mWorkspace.setStateWithAnimation(toState, animated, layerViews);
        updateInteraction(fromState, toState);
        return anim;
    }

    public void enterSpringLoadedDragMode() {
        if (LOGD) Log.d(TAG, String.format("enterSpringLoadedDragMode [mState=%s", mState.name()));
        if (isStateSpringLoaded()) {
            return;
        }

        mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
                Workspace.State.SPRING_LOADED, true /* animated */,
                null /* onCompleteRunnable */);
        setState(State.WORKSPACE_SPRING_LOADED);
    }

    public void exitSpringLoadedDragModeDelayed(final boolean successfulDrop, int delay,
            final Runnable onCompleteRunnable) {
        if (!isStateSpringLoaded()) return;

        if (mExitSpringLoadedModeRunnable != null) {
            mHandler.removeCallbacks(mExitSpringLoadedModeRunnable);
        }
        mExitSpringLoadedModeRunnable = new Runnable() {
            @Override
            public void run() {
                if (successfulDrop) {
                    // TODO(hyunyoungs): verify if this hack is still needed, if not, delete.
                    //
                    // Before we show workspace, hide all apps again because
                    // exitSpringLoadedDragMode made it visible. This is a bit hacky; we should
                    // clean up our state transition functions
                    mWidgetsView.setVisibility(View.GONE);
                    showWorkspace(true, onCompleteRunnable);
                } else {
                    exitSpringLoadedDragMode();
                }
                mExitSpringLoadedModeRunnable = null;
            }
        };
        mHandler.postDelayed(mExitSpringLoadedModeRunnable, delay);
    }

    boolean isStateSpringLoaded() {
        return mState == State.WORKSPACE_SPRING_LOADED || mState == State.APPS_SPRING_LOADED
                || mState == State.WIDGETS_SPRING_LOADED;
    }

    public void exitSpringLoadedDragMode() {
        if (mState == State.APPS_SPRING_LOADED) {
            showAppsView(true /* animated */,
                    false /* updatePredictedApps */, false /* focusSearchBar */);
        } else if (mState == State.WIDGETS_SPRING_LOADED) {
            showWidgetsView(true, false);
        } else if (mState == State.WORKSPACE_SPRING_LOADED) {
            showWorkspace(true);
        }
    }

    /**
     * Updates the set of predicted apps if it hasn't been updated since the last time Launcher was
     * resumed.
     */
    public void tryAndUpdatePredictedApps() {
        if (mLauncherCallbacks != null) {
            List<ComponentKeyMapper<AppInfo>> apps = mLauncherCallbacks.getPredictedApps();
            if (apps != null) {
                mAppsView.setPredictedApps(apps);
            }
        }
    }

    void lockAllApps() {
        // TODO
    }

    void unlockAllApps() {
        // TODO
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        final boolean result = super.dispatchPopulateAccessibilityEvent(event);
        final List<CharSequence> text = event.getText();
        text.clear();
        // Populate event with a fake title based on the current state.
        if (mState == State.APPS) {
            text.add(getString(R.string.all_apps_button_label));
        } else if (mState == State.WIDGETS) {
            text.add(getString(R.string.widget_button_text));
        } else if (mWorkspace != null) {
            text.add(mWorkspace.getCurrentPageDescription());
        } else {
            text.add(getString(R.string.all_apps_home_button_label));
        }
        return result;
    }

    /**
     * If the activity is currently paused, signal that we need to run the passed Runnable
     * in onResume.
     *
     * This needs to be called from incoming places where resources might have been loaded
     * while the activity is paused. That is because the Configuration (e.g., rotation)  might be
     * wrong when we're not running, and if the activity comes back to what the configuration was
     * when we were paused, activity is not restarted.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @return {@code true} if we are currently paused. The caller might be able to skip some work
     */
    @Thunk boolean waitUntilResume(Runnable run) {
        if (mPaused) {
            if (LOGD) Log.d(TAG, "Deferring update until onResume");
            if (run instanceof RunnableWithId) {
                // Remove any runnables which have the same id
                while (mBindOnResumeCallbacks.remove(run)) { }
            }
            mBindOnResumeCallbacks.add(run);
            return true;
        } else {
            return false;
        }
    }

    public void addOnResumeCallback(Runnable run) {
        mOnResumeCallbacks.add(run);
    }

    /**
     * If the activity is currently paused, signal that we need to re-run the loader
     * in onResume.
     *
     * This needs to be called from incoming places where resources might have been loaded
     * while we are paused.  That is becaues the Configuration might be wrong
     * when we're not running, and if it comes back to what it was when we
     * were paused, we are not restarted.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @return true if we are currently paused.  The caller might be able to
     * skip some work in that case since we will come back again.
     */
    @Override
    public boolean setLoadOnResume() {
        if (mPaused) {
            if (LOGD) Log.d(TAG, "setLoadOnResume");
            mOnResumeNeedsLoad = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public int getCurrentWorkspaceScreen() {
        if (mWorkspace != null) {
            return mWorkspace.getCurrentPage();
        } else {
            return 0;
        }
    }

    /**
     * Clear any pending bind callbacks. This is called when is loader is planning to
     * perform a full rebind from scratch.
     */
    @Override
    public void clearPendingBinds() {
        mBindOnResumeCallbacks.clear();
        if (mPendingExecutor != null) {
            mPendingExecutor.markCompleted();
            mPendingExecutor = null;
        }
    }

    /**
     * Refreshes the shortcuts shown on the workspace.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void startBinding() {
        if (LauncherAppState.PROFILE_STARTUP) {
            Trace.beginSection("Starting page bind");
        }

        AbstractFloatingView.closeAllOpenViews(this);

        setWorkspaceLoading(true);

        // Clear the workspace because it's going to be rebound
        mWorkspace.clearDropTargets();
        mWorkspace.removeAllWorkspaceScreens();

        if (mHotseat != null) {
            mHotseat.resetLayout();
        }
        if (LauncherAppState.PROFILE_STARTUP) {
            Trace.endSection();
        }
    }

    @Override
    public void bindScreens(ArrayList<Long> orderedScreenIds) {
        // Make sure the first screen is always at the start.
        if (FeatureFlags.QSB_ON_FIRST_SCREEN &&
                orderedScreenIds.indexOf(Workspace.FIRST_SCREEN_ID) != 0) {
            orderedScreenIds.remove(Workspace.FIRST_SCREEN_ID);
            orderedScreenIds.add(0, Workspace.FIRST_SCREEN_ID);
            LauncherModel.updateWorkspaceScreenOrder(this, orderedScreenIds);
        } else if (!FeatureFlags.QSB_ON_FIRST_SCREEN && orderedScreenIds.isEmpty()) {
            // If there are no screens, we need to have an empty screen
            mWorkspace.addExtraEmptyScreen();
        }
        bindAddScreens(orderedScreenIds);

        // Create the custom content page (this call updates mDefaultScreen which calls
        // setCurrentPage() so ensure that all pages are added before calling this).
        if (hasCustomContentToLeft()) {
            mWorkspace.createCustomContentContainer();
            populateCustomContentContainer();
        }

        // After we have added all the screens, if the wallpaper was locked to the default state,
        // then notify to indicate that it can be released and a proper wallpaper offset can be
        // computed before the next layout
        mWorkspace.unlockWallpaperFromDefaultPageOnNextLayout();
    }

    private void bindAddScreens(ArrayList<Long> orderedScreenIds) {
        int count = orderedScreenIds.size();
        for (int i = 0; i < count; i++) {
            long screenId = orderedScreenIds.get(i);
            if (!FeatureFlags.QSB_ON_FIRST_SCREEN || screenId != Workspace.FIRST_SCREEN_ID) {
                // No need to bind the first screen, as its always bound.
                mWorkspace.insertNewWorkspaceScreenBeforeEmptyScreen(screenId);
            }
        }
    }

    @Override
    public void bindAppsAdded(final ArrayList<Long> newScreens,
                              final ArrayList<ItemInfo> addNotAnimated,
                              final ArrayList<ItemInfo> addAnimated) {
        Runnable r = new Runnable() {
            public void run() {
                bindAppsAdded(newScreens, addNotAnimated, addAnimated);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        // Add the new screens
        if (newScreens != null) {
            bindAddScreens(newScreens);
        }

        // We add the items without animation on non-visible pages, and with
        // animations on the new page (which we will try and snap to).
        if (addNotAnimated != null && !addNotAnimated.isEmpty()) {
            bindItems(addNotAnimated, false);
        }
        if (addAnimated != null && !addAnimated.isEmpty()) {
            bindItems(addAnimated, true);
        }

        // Remove the extra empty screen
        mWorkspace.removeExtraEmptyScreen(false, false);
    }

    /**
     * Bind the items start-end from the list.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindItems(final List<ItemInfo> items, final boolean forceAnimateIcons) {
        Runnable r = new Runnable() {
            public void run() {
                bindItems(items, forceAnimateIcons);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        // Get the list of added items and intersect them with the set of items here
        final AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        final Collection<Animator> bounceAnims = new ArrayList<>();
        final boolean animateIcons = forceAnimateIcons && canRunNewAppsAnimation();
        Workspace workspace = mWorkspace;
        long newItemsScreenId = -1;
        int end = items.size();
        for (int i = 0; i < end; i++) {
            final ItemInfo item = items.get(i);

            // Short circuit if we are loading dock items for a configuration which has no dock
            if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT &&
                    mHotseat == null) {
                continue;
            }

            final View view;
            switch (item.itemType) {
                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT: {
                    ShortcutInfo info = (ShortcutInfo) item;
                    view = createShortcut(info);
                    break;
                }
                case LauncherSettings.Favorites.ITEM_TYPE_FOLDER: {
                    view = FolderIcon.fromXml(R.layout.folder_icon, this,
                            (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()),
                            (FolderInfo) item);
                    break;
                }
                case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET: {
                    view = inflateAppWidget((LauncherAppWidgetInfo) item);
                    if (view == null) {
                        continue;
                    }
                    break;
                }
                default:
                    throw new RuntimeException("Invalid Item Type");
            }

             /*
             * Remove colliding items.
             */
            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                CellLayout cl = mWorkspace.getScreenWithId(item.screenId);
                if (cl != null && cl.isOccupied(item.cellX, item.cellY)) {
                    View v = cl.getChildAt(item.cellX, item.cellY);
                    Object tag = v.getTag();
                    String desc = "Collision while binding workspace item: " + item
                            + ". Collides with " + tag;
                    if (FeatureFlags.IS_DOGFOOD_BUILD) {
                        throw (new RuntimeException(desc));
                    } else {
                        Log.d(TAG, desc);
                        getModelWriter().deleteItemFromDatabase(item);
                        continue;
                    }
                }
            }
            workspace.addInScreenFromBind(view, item);
            if (animateIcons) {
                // Animate all the applications up now
                view.setAlpha(0f);
                view.setScaleX(0f);
                view.setScaleY(0f);
                bounceAnims.add(createNewAppBounceAnimation(view, i));
                newItemsScreenId = item.screenId;
            }
        }

        if (animateIcons) {
            // Animate to the correct page
            if (newItemsScreenId > -1) {
                long currentScreenId = mWorkspace.getScreenIdForPageIndex(mWorkspace.getNextPage());
                final int newScreenIndex = mWorkspace.getPageIndexForScreenId(newItemsScreenId);
                final Runnable startBounceAnimRunnable = new Runnable() {
                    public void run() {
                        anim.playTogether(bounceAnims);
                        anim.start();
                    }
                };
                if (newItemsScreenId != currentScreenId) {
                    // We post the animation slightly delayed to prevent slowdowns
                    // when we are loading right after we return to launcher.
                    mWorkspace.postDelayed(new Runnable() {
                        public void run() {
                            if (mWorkspace != null) {
                                mWorkspace.snapToPage(newScreenIndex);
                                mWorkspace.postDelayed(startBounceAnimRunnable,
                                        NEW_APPS_ANIMATION_DELAY);
                            }
                        }
                    }, NEW_APPS_PAGE_MOVE_DELAY);
                } else {
                    mWorkspace.postDelayed(startBounceAnimRunnable, NEW_APPS_ANIMATION_DELAY);
                }
            }
        }
        workspace.requestLayout();
    }

    /**
     * Add the views for a widget to the workspace.
     */
    public void bindAppWidget(LauncherAppWidgetInfo item) {
        View view = inflateAppWidget(item);
        if (view != null) {
            mWorkspace.addInScreen(view, item);
            mWorkspace.requestLayout();
        }
    }

    private View inflateAppWidget(LauncherAppWidgetInfo item) {
        if (mIsSafeModeEnabled) {
            PendingAppWidgetHostView view =
                    new PendingAppWidgetHostView(this, item, mIconCache, true);
            prepareAppWidget(view, item);
            return view;
        }

        final long start = DEBUG_WIDGETS ? SystemClock.uptimeMillis() : 0;
        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bindAppWidget: " + item);
        }

        final LauncherAppWidgetProviderInfo appWidgetInfo;

        if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY)) {
            // If the provider is not ready, bind as a pending widget.
            appWidgetInfo = null;
        } else if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_NOT_VALID)) {
            // The widget id is not valid. Try to find the widget based on the provider info.
            appWidgetInfo = mAppWidgetManager.findProvider(item.providerName, item.user);
        } else {
            appWidgetInfo = mAppWidgetManager.getLauncherAppWidgetInfo(item.appWidgetId);
        }

        // If the provider is ready, but the width is not yet restored, try to restore it.
        if (!item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY) &&
                (item.restoreStatus != LauncherAppWidgetInfo.RESTORE_COMPLETED)) {
            if (appWidgetInfo == null) {
                if (DEBUG_WIDGETS) {
                    Log.d(TAG, "Removing restored widget: id=" + item.appWidgetId
                            + " belongs to component " + item.providerName
                            + ", as the provider is null");
                }
                getModelWriter().deleteItemFromDatabase(item);
                return null;
            }

            // If we do not have a valid id, try to bind an id.
            if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_NOT_VALID)) {
                if (!item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_ALLOCATED)) {
                    // Id has not been allocated yet. Allocate a new id.
                    item.appWidgetId = mAppWidgetHost.allocateAppWidgetId();
                    item.restoreStatus |= LauncherAppWidgetInfo.FLAG_ID_ALLOCATED;

                    // Also try to bind the widget. If the bind fails, the user will be shown
                    // a click to setup UI, which will ask for the bind permission.
                    PendingAddWidgetInfo pendingInfo = new PendingAddWidgetInfo(appWidgetInfo);
                    pendingInfo.spanX = item.spanX;
                    pendingInfo.spanY = item.spanY;
                    pendingInfo.minSpanX = item.minSpanX;
                    pendingInfo.minSpanY = item.minSpanY;
                    Bundle options = WidgetHostViewLoader.getDefaultOptionsForWidget(this, pendingInfo);

                    boolean isDirectConfig =
                            item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_DIRECT_CONFIG);
                    if (isDirectConfig && item.bindOptions != null) {
                        Bundle newOptions = item.bindOptions.getExtras();
                        if (options != null) {
                            newOptions.putAll(options);
                        }
                        options = newOptions;
                    }
                    boolean success = mAppWidgetManager.bindAppWidgetIdIfAllowed(
                            item.appWidgetId, appWidgetInfo, options);

                    // We tried to bind once. If we were not able to bind, we would need to
                    // go through the permission dialog, which means we cannot skip the config
                    // activity.
                    item.bindOptions = null;
                    item.restoreStatus &= ~LauncherAppWidgetInfo.FLAG_DIRECT_CONFIG;

                    // Bind succeeded
                    if (success) {
                        // If the widget has a configure activity, it is still needs to set it up,
                        // otherwise the widget is ready to go.
                        item.restoreStatus = (appWidgetInfo.configure == null) || isDirectConfig
                                ? LauncherAppWidgetInfo.RESTORE_COMPLETED
                                : LauncherAppWidgetInfo.FLAG_UI_NOT_READY;
                    }

                    getModelWriter().updateItemInDatabase(item);
                }
            } else if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_UI_NOT_READY)
                    && (appWidgetInfo.configure == null)) {
                // The widget was marked as UI not ready, but there is no configure activity to
                // update the UI.
                item.restoreStatus = LauncherAppWidgetInfo.RESTORE_COMPLETED;
                getModelWriter().updateItemInDatabase(item);
            }
        }

        final AppWidgetHostView view;
        if (item.restoreStatus == LauncherAppWidgetInfo.RESTORE_COMPLETED) {
            if (DEBUG_WIDGETS) {
                Log.d(TAG, "bindAppWidget: id=" + item.appWidgetId + " belongs to component "
                        + appWidgetInfo.provider);
            }

            // Verify that we own the widget
            if (appWidgetInfo == null) {
                FileLog.e(TAG, "Removing invalid widget: id=" + item.appWidgetId);
                deleteWidgetInfo(item);
                return null;
            }

            item.minSpanX = appWidgetInfo.minSpanX;
            item.minSpanY = appWidgetInfo.minSpanY;
            view = mAppWidgetHost.createView(this, item.appWidgetId, appWidgetInfo);
        } else {
            view = new PendingAppWidgetHostView(this, item, mIconCache, false);
        }
        prepareAppWidget(view, item);

        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bound widget id="+item.appWidgetId+" in "
                    + (SystemClock.uptimeMillis()-start) + "ms");
        }
        return view;
    }

    /**
     * Restores a pending widget.
     *
     * @param appWidgetId The app widget id
     */
    private LauncherAppWidgetInfo completeRestoreAppWidget(int appWidgetId, int finalRestoreFlag) {
        LauncherAppWidgetHostView view = mWorkspace.getWidgetForAppWidgetId(appWidgetId);
        if ((view == null) || !(view instanceof PendingAppWidgetHostView)) {
            Log.e(TAG, "Widget update called, when the widget no longer exists.");
            return null;
        }

        LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) view.getTag();
        info.restoreStatus = finalRestoreFlag;
        if (info.restoreStatus == LauncherAppWidgetInfo.RESTORE_COMPLETED) {
            info.pendingItemInfo = null;
        }

        mWorkspace.reinflateWidgetsIfNecessary();
        getModelWriter().updateItemInDatabase(info);
        return info;
    }

    public void onPageBoundSynchronously(int page) {
        mSynchronouslyBoundPages.add(page);
    }

    @Override
    public void executeOnNextDraw(ViewOnDrawExecutor executor) {
        if (mPendingExecutor != null) {
            mPendingExecutor.markCompleted();
        }
        mPendingExecutor = executor;
        executor.attachTo(this);
    }

    public void clearPendingExecutor(ViewOnDrawExecutor executor) {
        if (mPendingExecutor == executor) {
            mPendingExecutor = null;
        }
    }

    @Override
    public void finishFirstPageBind(final ViewOnDrawExecutor executor) {
        Runnable r = new Runnable() {
            public void run() {
                finishFirstPageBind(executor);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        Runnable onComplete = new Runnable() {
            @Override
            public void run() {
                if (executor != null) {
                    executor.onLoadAnimationCompleted();
                }
            }
        };
        if (mDragLayer.getAlpha() < 1) {
            mDragLayer.animate().alpha(1).withEndAction(onComplete).start();
        } else {
            onComplete.run();
        }
    }

    /**
     * Callback saying that there aren't any more items to bind.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void finishBindingItems() {
        Runnable r = new Runnable() {
            public void run() {
                finishBindingItems();
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        if (LauncherAppState.PROFILE_STARTUP) {
            Trace.beginSection("Page bind completed");
        }
        mWorkspace.restoreInstanceStateForRemainingPages();

        setWorkspaceLoading(false);

        if (mPendingActivityResult != null) {
            handleActivityResult(mPendingActivityResult.requestCode,
                    mPendingActivityResult.resultCode, mPendingActivityResult.data);
            mPendingActivityResult = null;
        }

        InstallShortcutReceiver.disableAndFlushInstallQueue(
                InstallShortcutReceiver.FLAG_LOADER_RUNNING, this);

        NotificationListener.setNotificationsChangedListener(mPopupDataProvider);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.finishBindingItems(false);
        }
        if (LauncherAppState.PROFILE_STARTUP) {
            Trace.endSection();
        }
    }

    private boolean canRunNewAppsAnimation() {
        long diff = System.currentTimeMillis() - mDragController.getLastGestureUpTime();
        return diff > (NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS * 1000);
    }

    private ValueAnimator createNewAppBounceAnimation(View v, int i) {
        ValueAnimator bounceAnim = LauncherAnimUtils.ofViewAlphaAndScale(v, 1, 1, 1);
        bounceAnim.setDuration(InstallShortcutReceiver.NEW_SHORTCUT_BOUNCE_DURATION);
        bounceAnim.setStartDelay(i * InstallShortcutReceiver.NEW_SHORTCUT_STAGGER_DELAY);
        bounceAnim.setInterpolator(new OvershootInterpolator(BOUNCE_ANIMATION_TENSION));
        return bounceAnim;
    }

    public boolean useVerticalBarLayout() {
        return mDeviceProfile.isVerticalBarLayout();
    }

    public int getSearchBarHeight() {
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.getSearchBarHeight();
        }
        return LauncherCallbacks.SEARCH_BAR_HEIGHT_NORMAL;
    }

    /**
     * Add the icons for all apps.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAllApplications(final ArrayList<AppInfo> apps) {
        Runnable r = new RunnableWithId(RUNNABLE_ID_BIND_APPS) {
            public void run() {
                bindAllApplications(apps);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        if (mAppsView != null) {
            Executor pendingExecutor = getPendingExecutor();
            if (pendingExecutor != null && mState != State.APPS) {
                // Wait until the fade in animation has finished before setting all apps list.
                pendingExecutor.execute(r);
                return;
            }

            mAppsView.setApps(apps);
        }
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.bindAllApplications(apps);
        }
    }

    /**
     * Returns an Executor that will run after the launcher is first drawn (including after the
     * initial fade in animation). Returns null if the first draw has already occurred.
     */
    public @Nullable Executor getPendingExecutor() {
        return mPendingExecutor != null && mPendingExecutor.canQueue() ? mPendingExecutor : null;
    }

    /**
     * Copies LauncherModel's map of activities to shortcut ids to Launcher's. This is necessary
     * because LauncherModel's map is updated in the background, while Launcher runs on the UI.
     */
    @Override
    public void bindDeepShortcutMap(MultiHashMap<ComponentKey, String> deepShortcutMapCopy) {
        mPopupDataProvider.setDeepShortcutMap(deepShortcutMapCopy);
    }

    /**
     * A package was updated.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsAddedOrUpdated(final ArrayList<AppInfo> apps) {
        Runnable r = new Runnable() {
            public void run() {
                bindAppsAddedOrUpdated(apps);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        if (mAppsView != null) {
            mAppsView.addOrUpdateApps(apps);
        }
        fetchAllAppList();
    }

    @Override
    public void bindPromiseAppProgressUpdated(final PromiseAppInfo app) {
        Runnable r = new Runnable() {
            public void run() {
                bindPromiseAppProgressUpdated(app);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        if (mAppsView != null) {
            mAppsView.updatePromiseAppProgress(app);
        }
    }

    @Override
    public void bindWidgetsRestored(final ArrayList<LauncherAppWidgetInfo> widgets) {
        Runnable r = new Runnable() {
            public void run() {
                bindWidgetsRestored(widgets);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        mWorkspace.widgetsRestored(widgets);
    }

    /**
     * Some shortcuts were updated in the background.
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @param updated list of shortcuts which have changed.
     */
    @Override
    public void bindShortcutsChanged(final ArrayList<ShortcutInfo> updated, final UserHandle user) {
        Runnable r = new Runnable() {
            public void run() {
                bindShortcutsChanged(updated, user);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        if (!updated.isEmpty()) {
            mWorkspace.updateShortcuts(updated);
        }
    }

    /**
     * Update the state of a package, typically related to install state.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindRestoreItemsChange(final HashSet<ItemInfo> updates) {
        Runnable r = new Runnable() {
            public void run() {
                bindRestoreItemsChange(updates);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        mWorkspace.updateRestoreItems(updates);
    }

    /**
     * A package was uninstalled/updated.  We take both the super set of packageNames
     * in addition to specific applications to remove, the reason being that
     * this can be called when a package is updated as well.  In that scenario,
     * we only remove specific components from the workspace and hotseat, where as
     * package-removal should clear all items by package name.
     */
    @Override
    public void bindWorkspaceComponentsRemoved(final ItemInfoMatcher matcher) {
        Runnable r = new Runnable() {
            public void run() {
                bindWorkspaceComponentsRemoved(matcher);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        mWorkspace.removeItemsByMatcher(matcher);
        mDragController.onAppsRemoved(matcher);
    }

    @Override
    public void bindAppInfosRemoved(final ArrayList<AppInfo> appInfos) {
        Runnable r = new Runnable() {
            public void run() {
                bindAppInfosRemoved(appInfos);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        fetchAllAppList();
        // Update AllApps
        if (mAppsView != null) {
            mAppsView.removeApps(appInfos);
            tryAndUpdatePredictedApps();
        }
    }

    @Override
    public void bindAllWidgets(final MultiHashMap<PackageItemInfo, WidgetItem> allWidgets) {
        Runnable r = new RunnableWithId(RUNNABLE_ID_BIND_WIDGETS) {
            @Override
            public void run() {
                bindAllWidgets(allWidgets);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        if (mWidgetsView != null && allWidgets != null) {
            Executor pendingExecutor = getPendingExecutor();
            if (pendingExecutor != null && mState != State.WIDGETS) {
                pendingExecutor.execute(r);
                return;
            }
            mWidgetsView.setWidgets(allWidgets);
        }

        AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(this);
        if (topView != null) {
            topView.onWidgetsBound();
        }
    }

    public List<WidgetItem> getWidgetsForPackageUser(PackageUserKey packageUserKey) {
        return mWidgetsView.getWidgetsForPackageUser(packageUserKey);
    }

    @Override
    public void notifyWidgetProvidersChanged() {
        if (mWorkspace.getState().shouldUpdateWidget) {
            refreshAndBindWidgetsForPackageUser(null);
        }
    }

    /**
     * @param packageUser if null, refreshes all widgets and shortcuts, otherwise only
     *                    refreshes the widgets and shortcuts associated with the given package/user
     */
    public void refreshAndBindWidgetsForPackageUser(@Nullable PackageUserKey packageUser) {
        mModel.refreshAndBindWidgetsAndShortcuts(packageUser);
    }

    public void lockScreenOrientation() {
        if (mRotationEnabled) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }
    }

    public void unlockScreenOrientation(boolean immediate) {
        if (mRotationEnabled) {
            if (immediate) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            } else {
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }
                }, RESTORE_SCREEN_ORIENTATION_DELAY);
            }
        }
    }

    private void markAppsViewShown() {
        if (mSharedPrefs.getBoolean(APPS_VIEW_SHOWN, false)) {
            return;
        }
        mSharedPrefs.edit().putBoolean(APPS_VIEW_SHOWN, true).apply();
    }

    private boolean shouldShowDiscoveryBounce() {
        UserManagerCompat um = UserManagerCompat.getInstance(this);
        return mState == State.WORKSPACE && !mSharedPrefs.getBoolean(APPS_VIEW_SHOWN, false) && !um.isDemoUser();
    }

    protected void moveWorkspaceToDefaultScreen() {
        mWorkspace.moveToDefaultScreen(false);
    }

    /**
     * $ adb shell dumpsys activity com.android.launcher3.Launcher [--all]
     */
    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);

        if (args.length > 0 && TextUtils.equals(args[0], "--all")) {
            writer.println(prefix + "Workspace Items");
            for (int i = mWorkspace.numCustomPages(); i < mWorkspace.getPageCount(); i++) {
                writer.println(prefix + "  Homescreen " + i);

                ViewGroup layout = ((CellLayout) mWorkspace.getPageAt(i)).getShortcutsAndWidgets();
                for (int j = 0; j < layout.getChildCount(); j++) {
                    Object tag = layout.getChildAt(j).getTag();
                    if (tag != null) {
                        writer.println(prefix + "    " + tag.toString());
                    }
                }
            }

            writer.println(prefix + "  Hotseat");
            ViewGroup layout = mHotseat.getLayout().getShortcutsAndWidgets();
            for (int j = 0; j < layout.getChildCount(); j++) {
                Object tag = layout.getChildAt(j).getTag();
                if (tag != null) {
                    writer.println(prefix + "    " + tag.toString());
                }
            }

            try {
                FileLog.flushAll(writer);
            } catch (Exception e) {
                // Ignore
            }
        }

        writer.println(prefix + "Misc:");
        writer.print(prefix + "\tmWorkspaceLoading=" + mWorkspaceLoading);
        writer.print(" mPendingRequestArgs=" + mPendingRequestArgs);
        writer.println(" mPendingActivityResult=" + mPendingActivityResult);

        mModel.dumpState(prefix, fd, writer, args);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.dump(prefix, fd, writer, args);
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.N)
    public void onProvideKeyboardShortcuts(
            List<KeyboardShortcutGroup> data, Menu menu, int deviceId) {

        ArrayList<KeyboardShortcutInfo> shortcutInfos = new ArrayList<>();
        if (mState == State.WORKSPACE) {
            shortcutInfos.add(new KeyboardShortcutInfo(getString(R.string.all_apps_button_label),
                    KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON));
        }
        View currentFocus = getCurrentFocus();
        if (new CustomActionsPopup(this, currentFocus).canShow()) {
            shortcutInfos.add(new KeyboardShortcutInfo(getString(R.string.custom_actions),
                    KeyEvent.KEYCODE_O, KeyEvent.META_CTRL_ON));
        }
        if (currentFocus.getTag() instanceof ItemInfo
                && DeepShortcutManager.supportsShortcuts((ItemInfo) currentFocus.getTag())) {
            shortcutInfos.add(new KeyboardShortcutInfo(getString(R.string.action_deep_shortcut),
                    KeyEvent.KEYCODE_S, KeyEvent.META_CTRL_ON));
        }
        if (!shortcutInfos.isEmpty()) {
            data.add(new KeyboardShortcutGroup(getString(R.string.home_screen), shortcutInfos));
        }

        super.onProvideKeyboardShortcuts(data, menu, deviceId);
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        if (event.hasModifiers(KeyEvent.META_CTRL_ON)) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_A:
                    if (mState == State.WORKSPACE) {
                        showAppsView(true, true, false);
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_S: {
                    View focusedView = getCurrentFocus();
                    if (focusedView instanceof BubbleTextView
                            && focusedView.getTag() instanceof ItemInfo
                            && mAccessibilityDelegate.performAction(focusedView,
                                    (ItemInfo) focusedView.getTag(),
                                    LauncherAccessibilityDelegate.DEEP_SHORTCUTS)) {
                        PopupContainerWithArrow.getOpen(this).requestFocus();
                        return true;
                    }
                    break;
                }
                case KeyEvent.KEYCODE_O:
                    if (new CustomActionsPopup(this, getCurrentFocus()).show()) {
                        return true;
                    }
                    break;
            }
        }
        return super.onKeyShortcut(keyCode, event);
    }

    public static CustomAppWidget getCustomAppWidget(String name) {
        return sCustomAppWidgets.get(name);
    }

    public static HashMap<String, CustomAppWidget> getCustomAppWidgets() {
        return sCustomAppWidgets;
    }

    public static Launcher getLauncher(Context context) {
        if (context instanceof Launcher) {
            return (Launcher) context;
        }
        return ((Launcher) ((ContextWrapper) context).getBaseContext());
    }

    private class WallpaperButtonClickedHandler implements OnSharedPreferenceChangeListener {
        View view = findViewById(R.id.launcher);
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(key.equals(ProfilesActivity.WALLPAPER_BTN_CLICKED)){
                onRequestWallpaperPick();
            }
        }
    }

    private class SchedulePrefChangeHandler implements OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(key.equals(TimePreferenceActivity.SCHEDULE_PREF)){
                AlarmsService.updateAlarmsList();
            }
        }
    }

    private class MinimalDesignPrefChangeHandler implements OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            for (String profile : availableProfiles) {
                if(profile.equals("home") || profile.equals("work") || profile.equals("default") ||profile.equals("disconnected") ){
                    if(key.equals(profile+ProfilesActivity.MINIMAL_DESIGN_PREF)){
                        String currentProfile = mSharedPrefs.getString(CURRENT_PROFILE_PREF, "default");
                        if (profile.equals(currentProfile)) {
                            isMinimalDesignON = mSharedPrefs.getBoolean(profile + ProfilesActivity.MINIMAL_DESIGN_PREF, false);
                            switchToMinimalLayout(isMinimalDesignON);
                        }
                    }
                } else {
                    Set set = mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
                    if(set!=null){
                        newAddedProfiles = new ArrayList<>(set);
                        if(newAddedProfiles!=null){
                            for(String sub : newAddedProfiles){
                                if(sub.length()!=0){
                                    if(sub.substring(1).equals(profile)){
                                        String profileID = sub.charAt(0)+"";
                                        if(key.equals(profileID+ProfilesActivity.MINIMAL_DESIGN_PREF)){
                                            String currentProfile = mSharedPrefs.getString(CURRENT_PROFILE_PREF, "default");
                                            if (profile.equals(currentProfile)) {
                                                isMinimalDesignON = mSharedPrefs.getBoolean(profileID + ProfilesActivity.MINIMAL_DESIGN_PREF, false);
                                                switchToMinimalLayout(isMinimalDesignON);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private class RotationPrefChangeHandler implements OnSharedPreferenceChangeListener {

        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            if (Utilities.ALLOW_ROTATION_PREFERENCE_KEY.equals(key)) {
                // Recreate the activity so that it initializes the rotation preference again.
                recreate();
            }
        }
    }

    private class ProfileChangeHandler implements OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(key.equals(MANUAL_PROFILE_PREF)){
                String selectedProfile = mSharedPrefs.getString(MANUAL_PROFILE_PREF, null);
                if(selectedProfile!=null){
                    selectedProfile = selectedProfile.split("_")[0];
                    LogEntryProfileTriggered logEntry = new LogEntryProfileTriggered(selectedProfile, "manually", "");
                    firebaseLogger.addLogMessage("events", "profile triggered", logEntry);
                    updateProfile(selectedProfile);
                }
            }
            if(key.equals(AlarmReceiver.CHANGE_PROFILE_ALARM)){
                String currentProfile = mSharedPrefs.getString(CURRENT_PROFILE_PREF, null);
                String updateToProfile = mSharedPrefs.getString(AlarmReceiver.CHANGE_PROFILE_ALARM, null);
                if(updateToProfile!= null){
                    updateToProfile = updateToProfile.split("_")[0];
                    if(!currentProfile.equals(updateToProfile)){
                        if(updateToProfile.equals("home") || updateToProfile.equals("work")){
                            LogEntryProfileTriggered logEntry = new LogEntryProfileTriggered(updateToProfile, "alarm", "");
                            firebaseLogger.addLogMessage("events", "profile triggered", logEntry);
                            updateProfile(updateToProfile);
                        } else {
                            Set set = mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
                            if(set!=null) {
                                newAddedProfiles = new ArrayList<String>(set);
                                if (newAddedProfiles != null) {
                                    for(String sub : newAddedProfiles){
                                        if(updateToProfile.equals(sub.charAt(0)+"")){
                                            updateToProfile = sub.substring(1);
                                            LogEntryProfileTriggered logEntry = new LogEntryProfileTriggered(updateToProfile, "alarm", "");
                                            firebaseLogger.addLogMessage("events", "profile triggered", logEntry);
                                            updateProfile(updateToProfile);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(key.equals(CURRENT_PROFILE_PREF)){
                String currentProfile = mSharedPrefs.getString(CURRENT_PROFILE_PREF, null);
                if(currentProfile!=null){
                    if(currentProfile.equals("home") || currentProfile.equals("work") || currentProfile.equals("default") ||currentProfile.equals("disconnected") ){
                        isMinimalDesignON = mSharedPrefs.getBoolean(currentProfile + ProfilesActivity.MINIMAL_DESIGN_PREF, false);
                        if(isMinimalDesignON){ fetchHomescreenAppList(); }
                    } else {
                        Set set = mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
                        if(set!=null){
                            newAddedProfiles = new ArrayList<String>(set);
                            if(newAddedProfiles!=null){
                                for(String sub : newAddedProfiles){
                                    if(sub.substring(1).equals(currentProfile)){
                                        String profileID = sub.charAt(0)+"";
                                        isMinimalDesignON = mSharedPrefs.getBoolean(profileID + ProfilesActivity.MINIMAL_DESIGN_PREF, false);
                                    }
                                }
                            }
                        }
                        if(isMinimalDesignON){ fetchHomescreenAppList(); }
                    }
                }
            }
            if(key.equals(APPS_ON_HOMESCREEN)){
                if(isMinimalDesignON) {fetchHomescreenAppList();}
            }
        }
    }

    private class SSIDPrefChangeHandler implements OnSharedPreferenceChangeListener {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            final String[] keyParts = key.split("_");
            if (keyParts.length >= 2 && keyParts[1].equals("ssids")) {
                final String ssid = getCurrentSSID(Launcher.this);
                if(keyParts[0].length()>1){
                    LogEntryProfileEdited logEntry = new LogEntryProfileEdited(keyParts[0], "ssid edited", getSSIDPref(keyParts[0]), getSchedulePref(keyParts[0]), getRingtonePref(keyParts[0]), getNotificationSoundPref(keyParts[0]), getNotificationBlockedPref(keyParts[0]), getMinimalDesignPref(keyParts[0]), getHomeScreenAppsList(keyParts[0]), getWallpaperInfo(keyParts[0]), getGrayScalePref(keyParts[0]));
                    firebaseLogger.addLogMessage("events", "profile edited", logEntry);
                } else {
                    Set set = mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
                    if(set!=null){
                        ArrayList<String> newAddedProfiles = new ArrayList<>(set);
                        for(String newAddedProfile : newAddedProfiles){
                            if((newAddedProfile.charAt(0)+"").equals(keyParts[0])){
                                LogEntryProfileEdited logEntry = new LogEntryProfileEdited(newAddedProfile.substring(1), "ssid edited", getSSIDPref(keyParts[0]), getSchedulePref(keyParts[0]), getRingtonePref(keyParts[0]), getNotificationSoundPref(keyParts[0]), getNotificationBlockedPref(keyParts[0]), getMinimalDesignPref(keyParts[0]), getHomeScreenAppsList(keyParts[0]), getWallpaperInfo(keyParts[0]), getGrayScalePref(keyParts[0]));
                                firebaseLogger.addLogMessage("events", "profile edited", logEntry);
                            }
                        }
                    }
                }

                changeProfile(ssid);
            }
        }
    }

    private class LogProfileEditedHandler implements OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            final String[] keyParts = key.split("_");
            if(keyParts.length>=2){
                if(keyParts[1].equals("ringtone")){
                    String profile = keyParts[0];
                    if(profile.length()>1){
                        LogEntryProfileEdited logEntry = new LogEntryProfileEdited(profile, "ringtone edited", getSSIDPref(keyParts[0]), getSchedulePref(keyParts[0]), getRingtonePref(keyParts[0]), getNotificationSoundPref(keyParts[0]), getNotificationBlockedPref(keyParts[0]), getMinimalDesignPref(keyParts[0]), getHomeScreenAppsList(keyParts[0]), getWallpaperInfo(keyParts[0]), getGrayScalePref(keyParts[0]));
                        firebaseLogger.addLogMessage("events", "profile edited", logEntry);
                    } else {
                        for(String newAddedProfile : newAddedProfiles){
                            if((newAddedProfile.charAt(0)+"").equals(profile)){
                                profile = newAddedProfile.substring(1);
                                LogEntryProfileEdited logEntry = new LogEntryProfileEdited(profile, "ringtone edited", getSSIDPref(keyParts[0]), getSchedulePref(keyParts[0]), getRingtonePref(keyParts[0]), getNotificationSoundPref(keyParts[0]), getNotificationBlockedPref(keyParts[0]), getMinimalDesignPref(keyParts[0]), getHomeScreenAppsList(keyParts[0]), getWallpaperInfo(keyParts[0]), getGrayScalePref(keyParts[0]));
                                firebaseLogger.addLogMessage("events", "profile edited", logEntry);
                            }
                        }
                    }
                }
            }

            if(keyParts.length>=3){
                if(("_"+keyParts[1]+"_"+keyParts[2]).equals("_notification_sound")){
                    String profile = keyParts[0];
                    if(profile.length()>1){
                        LogEntryProfileEdited logEntry = new LogEntryProfileEdited(profile, "notification sound edited", getSSIDPref(keyParts[0]), getSchedulePref(keyParts[0]), getRingtonePref(keyParts[0]), getNotificationSoundPref(keyParts[0]), getNotificationBlockedPref(keyParts[0]), getMinimalDesignPref(keyParts[0]), getHomeScreenAppsList(keyParts[0]), getWallpaperInfo(keyParts[0]), getGrayScalePref(keyParts[0]));
                        firebaseLogger.addLogMessage("events", "profile edited", logEntry);
                    } else {
                        for(String newAddedProfile : newAddedProfiles){
                            if((newAddedProfile.charAt(0)+"").equals(profile)){
                                profile = newAddedProfile.substring(1);
                                LogEntryProfileEdited logEntry = new LogEntryProfileEdited(profile, "notification sound edited", getSSIDPref(keyParts[0]), getSchedulePref(keyParts[0]), getRingtonePref(keyParts[0]), getNotificationSoundPref(keyParts[0]), getNotificationBlockedPref(keyParts[0]), getMinimalDesignPref(keyParts[0]), getHomeScreenAppsList(keyParts[0]), getWallpaperInfo(keyParts[0]), getGrayScalePref(keyParts[0]));
                                firebaseLogger.addLogMessage("events", "profile edited", logEntry);
                            }
                        }
                    }
                }

                if(("_"+keyParts[1]+"_"+keyParts[2]).equals("_hide_notifications")){
                    String profile = keyParts[0];
                    if(profile.length()>1){
                        LogEntryProfileEdited logEntry = new LogEntryProfileEdited(profile, "notification block edited", getSSIDPref(keyParts[0]), getSchedulePref(keyParts[0]), getRingtonePref(keyParts[0]), getNotificationSoundPref(keyParts[0]), getNotificationBlockedPref(keyParts[0]), getMinimalDesignPref(keyParts[0]), getHomeScreenAppsList(keyParts[0]), getWallpaperInfo(keyParts[0]), getGrayScalePref(keyParts[0]));
                        firebaseLogger.addLogMessage("events", "profile edited", logEntry);
                    } else {
                        for(String newAddedProfile : newAddedProfiles){
                            if((newAddedProfile.charAt(0)+"").equals(profile)){
                                profile = newAddedProfile.substring(1);
                                LogEntryProfileEdited logEntry = new LogEntryProfileEdited(profile, "notification block edited", getSSIDPref(keyParts[0]), getSchedulePref(keyParts[0]), getRingtonePref(keyParts[0]), getNotificationSoundPref(keyParts[0]), getNotificationBlockedPref(keyParts[0]), getMinimalDesignPref(keyParts[0]), getHomeScreenAppsList(keyParts[0]), getWallpaperInfo(keyParts[0]), getGrayScalePref(keyParts[0]));
                                firebaseLogger.addLogMessage("events", "profile edited", logEntry);
                            }
                        }
                    }
                }
            }
        }
    }

    public static List<String> getSSIDPref(String profile){
        String ssID1 = mSharedPrefs.getString(profile+"_ssids", null);
        List<String> ssIDInfo = new ArrayList<>();
        if(ssID1!=null){
            String[] ssID = ssID1.split("\\n");
            for(String element : ssID){
                if(!element.equals("")){
                    ssIDInfo.add(element);
                }
            }
        }

        if(ssIDInfo.isEmpty()){
            ssIDInfo.add("empty");
        }
        return ssIDInfo;
    }

    public static List<String> getSchedulePref(String profile){
        Set scheduleSet = mSharedPrefs.getStringSet(TimePreferenceActivity.SCHEDULE_PREF, null);
        List<String> scheduleInfo = new ArrayList<>();
        if(scheduleSet!=null){
            ArrayList<String> scheduleArray = new ArrayList<>(scheduleSet);
            for(String eachSchedule : scheduleArray){
                String eachProfile = eachSchedule.split("_")[0];
                if(eachProfile.equals(profile)){
                    String daysString = eachSchedule.split("_")[1].substring(1, eachSchedule.split("_")[1].length()-1);
                    String[] days = daysString.split(",");
                    for(String day : days){
                        if((day.charAt(0)+"").equals(" ")){
                            day = day.substring(1);
                        }
                        scheduleInfo.add(day);
                    }
                    String time = eachSchedule.split("_")[2];
                    if(time.split(":")[1].length()==1){
                        Character lastChar = time.charAt(time.length()-1);
                        time = time.substring(0,time.length()-1);
                        time = time+"0"+lastChar;
                    }
                    scheduleInfo.add(time);
                    return scheduleInfo;
                }
            }
        }
        if(scheduleInfo.isEmpty()){
            scheduleInfo.add("empty");
        }
        return scheduleInfo;
    }

    public static String getRingtonePref(String profile){
        String ringtone = mSharedPrefs.getString(profile+"_ringtone", null);
        String ringtoneInfo = "";
        if(ringtone!=null){
            ringtoneInfo = ringtone;
        }
        return ringtoneInfo;
    }

    public static String getNotificationSoundPref(String profile){
        String notificationSound = mSharedPrefs.getString(profile+"_notification_sound", null);
        String notificationSoundInfo = "";
        if(notificationSound!=null){
            notificationSoundInfo = notificationSound;
        }
        return notificationSoundInfo;
    }

    public static boolean getNotificationBlockedPref(String profile){
        return mSharedPrefs.getBoolean(profile+"_hide_notifications", false);
    }

    public static boolean getMinimalDesignPref(String profile){
        return mSharedPrefs.getBoolean(profile+"_minimal_design", false);
    }

    public static List<String> getHomeScreenAppsList(String profile){
        Set homescreenAppsSet = mSharedPrefs.getStringSet(APPS_ON_HOMESCREEN, null);
        List<String> homescreenAppsInfo = new ArrayList<>();
        if(homescreenAppsSet!=null){
            ArrayList<String> homescreenAppsArray = new ArrayList<>(homescreenAppsSet);
            for(String homescreenAppProfile : homescreenAppsArray){
                if(homescreenAppProfile.split("_").length>1){
                    if(profile.equals(homescreenAppProfile.split("_")[0])){
                        String[] apps = homescreenAppProfile.split("_")[1].split(",");
                        for(String app : apps){
                            if(!app.equals("")){
                                homescreenAppsInfo.add(app);
                            }
                        }
                    }
                }
            }
        }
        if(homescreenAppsInfo.isEmpty()){
            homescreenAppsInfo.add("empty");
        }
        return homescreenAppsInfo;
    }

    public static boolean getGrayScalePref(String profile){
        boolean grayscaleInfo = false;
        if(ProfilesActivity.getGrayscaleInfo(profile).equals("true")){
            grayscaleInfo = true;
        }
        return grayscaleInfo;
    }

    public static String getProfileSettings(String profile){
        String ssID = mSharedPrefs.getString(profile+"_ssids", null);
        String ssIDInfo = "ssID: { }, ";
        if(ssID!=null) {
            ssIDInfo = "ssID: {" + ssID + "}, ";
        }

        Set scheduleSet = mSharedPrefs.getStringSet(TimePreferenceActivity.SCHEDULE_PREF, null);
        String scheduleInfo = "schedule: { }, ";
        if(scheduleSet!=null){
            ArrayList<String> scheduleArray = new ArrayList<>(scheduleSet);
            for(String eachSchedule : scheduleArray){
                String eachProfile = eachSchedule.split("_")[0];
                if(eachProfile.equals(profile)){
                    String days = eachSchedule.split("_")[1];
                    String time = eachSchedule.split("_")[2];
                    if(time.split(":")[1].length()==1){
                        Character lastChar = time.charAt(time.length()-1);
                        time = time.substring(0,time.length()-1);
                        time = time+"0"+lastChar;
                    }
                    scheduleInfo = "schedule: { "+days+", "+time+" }, ";
                }
            }
        }

        String ringtone = mSharedPrefs.getString(profile+"_ringtone", null);
        String ringtoneInfo = "ringtone: , ";
        if(ringtone!=null){
            ringtoneInfo = "ringtone: "+ringtone+", ";
        }

        String notificationSound = mSharedPrefs.getString(profile+"_notification_sound", null);
        String notificationSoundInfo = "notification sound: , ";
        if(notificationSound!=null){
            notificationSoundInfo = "notification sound: "+notificationSound+", ";
        }

        boolean notificationBlock = mSharedPrefs.getBoolean(profile+"_hide_notifications", false);
        String notificationBlockInfo = "notifications blocked: "+notificationBlock+", ";

        boolean minimalDesign = mSharedPrefs.getBoolean(profile+"_minimal_design", false);
        String minimalDesignInfo = "minimal design on: "+minimalDesign+", ";

        Set homescreenAppsSet = mSharedPrefs.getStringSet(APPS_ON_HOMESCREEN, null);
        String homescreenAppsInfo ="homescreen apps: empty, ";
        if(homescreenAppsSet!=null){
            ArrayList<String> homescreenAppsArray = new ArrayList<>(homescreenAppsSet);
            for(String homescreenAppProfile : homescreenAppsArray){
                if(homescreenAppProfile.split(",").length>1){
                    if(profile.equals(homescreenAppProfile.split("_")[0])){
                        homescreenAppsInfo = "homescreen apps: {"+homescreenAppProfile.split("_")[1]+"}, ";
                    }
                }
            }
        }

        String wallpaperInfo = "wallpaper: "+getWallpaperInfo(profile)+", ";

        String grayscaleInfo = "grayscale on: "+ProfilesActivity.getGrayscaleInfo(profile);

        String profileSetting = ssIDInfo+scheduleInfo+ringtoneInfo+notificationSoundInfo+notificationBlockInfo+minimalDesignInfo+homescreenAppsInfo+wallpaperInfo+grayscaleInfo;
        return profileSetting;
    }
}

