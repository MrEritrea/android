package com.codepath.chefster.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.codepath.chefster.R;
import com.codepath.chefster.adapters.DishesAdapter;
import com.codepath.chefster.adapters.ViewPagerAdapter;
import com.codepath.chefster.client.FirebaseClient;
import com.codepath.chefster.fragments.ContainerFragment;
import com.codepath.chefster.fragments.DishesFragment;
import com.codepath.chefster.fragments.MainFragment;
import com.codepath.chefster.models.Dish;
import com.codepath.chefster.models.Dish_Table;
import com.codepath.chefster.models.Dishes;
import com.facebook.login.LoginManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class MainActivity extends BaseActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        DishesAdapter.DishesInteractionListener,
        MainFragment.CategoryClickedListener {
    private static final String ANONYMOUS = "anonymousUser";
    final static public String DISH_KEY = "selected_dish";
    private static final String CATEGORY_KEY = "category_name";

    @BindView(R.id.main_viewpager) ViewPager viewPager;
    @BindView(R.id.main_tab_layout) TabLayout tabLayout;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.search_view) MaterialSearchView searchView;
    @BindView(R.id.button_items_on_list) Button itemsOnListButton;


    // Firebase instance variables
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private String mUsername;
    private String mPhotoUrl;
    private GoogleApiClient mGoogleApiClient;

    ViewPagerAdapter adapter;
    private DatabaseReference mDatabase;
    private Dishes dishes;
    List<Dish> search_result;
    public List<Dish> selectedDishes;

    private FirebaseClient firebaseClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);
 /*       mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this *//* FragmentActivity *//*, this /* OnConnectionFailedListener *//*)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();
                */
        handleUserLogIn();

        // Use this call only if you have new Data stored in .json and you want to update the DB.
        // loadDataToDatabase();

        // Get all dishes from database.
        loadDishes();
    }

    // using this Method will upload new .json to DataBase and Overwrite the tree.
    private void loadDataToDatabase() {
        firebaseClient = new FirebaseClient(this);
        firebaseClient.uploadNewDataToDatabase();
    }

    public void loadDishes() {
        dishes = new Dishes();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        DatabaseReference ref = mDatabase.child("dishes");
        ref.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot dishSnapshot : dataSnapshot.getChildren()) {
                            Dish dish = dishSnapshot.getValue(Dish.class);
                            dish.save();
                            dishes.addDish(dish);
                        }
                        FirebaseClient.setDishes(dishes.getDishes());
                        setViewPager();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //handle databaseError
                    }
                });

        selectedDishes = new ArrayList<>();
    }

    private void setViewPager() {
        adapter = new ViewPagerAdapter(getSupportFragmentManager(), MainActivity.this);
        adapter.setFragment(ContainerFragment.newInstance(), 0);
        adapter.setFragment(DishesFragment.newInstance(null), 1);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void handleUserLogIn() {
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            mUsername = firebaseUser.getDisplayName();
            if (firebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = firebaseUser.getPhotoUrl().toString();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out:
                firebaseAuth.signOut();
                LoginManager.getInstance().logOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_home, menu);

        MenuItem item = menu.findItem(R.id.action_search);
        searchViewHandler(item);

        return true;
    }

    /*
    * this method handle the request from the searchView,
    * getting the query from the database and displaying the result.
    */
    private void searchViewHandler(MenuItem item) {
        searchView.setMenuItem(item);
        searchView.setVoiceSearch(true);
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                List<Dish> selectedDish  = SQLite.select().from(Dish.class)
                        .where(Dish_Table.title.like("%" + query + "%")).queryList();

                if (! selectedDish.isEmpty()) {
                    Dish currentDish = selectedDish.get(0);

                    // TODO - Add Lists to Table - DBFlow don't support Lists.
                    for ( Dish dish: dishes.getDishes() ) {
                        if (dish.getUid() == currentDish.getUid()){
                            currentDish = dish;
                        }
                    }

                    Intent intent = new Intent(getApplication(), DishDetailsActivity.class);
                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(DISH_KEY, Parcels.wrap(currentDish));
                    getApplication().startActivity(intent);
                    return true;
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                search_result = SQLite.select().from(Dish.class)
                        .where(Dish_Table.title.like("%" + newText + "%")).queryList();
                String[] strings = new String[search_result.size()];
                for (int i = 0; i < search_result.size(); i++) {
                    strings[i] = search_result.get(i).getTitle();
                }

                searchView.setSuggestions(strings);

                return true;
            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @OnClick(R.id.button_items_on_list)
    public void startCooking() {
        if (selectedDishes == null || selectedDishes.isEmpty()) {
            Toast.makeText(this, "There are 0 dishes on your list!", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(this, MealLaunchActivity.class);
            intent.putExtra("selected_dishes", Parcels.wrap(selectedDishes));
            startActivity(intent);
        }
    }

    @Override
    public void onDishLiked() {
        adapter.replaceFragment(DishesFragment.newInstance(null), 1);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDishAdded(Dish dish) {
        if (selectedDishes.size() == 3) {
            Toast.makeText(this, "Can't cook more than 3 dishes with this version", Toast.LENGTH_SHORT).show();
        } else {
            if (!selectedDishes.contains(dish)) {
                selectedDishes.add(dish);
            }
            itemsOnListButton.setText("Continue (" + selectedDishes.size() + " items)");
        }
    }

    @Override
    public void onDishRemoved(Dish dish) {
        selectedDishes.remove(dish);
        itemsOnListButton.setText("Dishes On List (" + selectedDishes.size() + ")");
    }

    @Override
    public void onCategoryclicked(String category) {
        adapter.replaceFragment(DishesFragment.newInstance(category), 0);
        adapter.notifyDataSetChanged();
    }
}
