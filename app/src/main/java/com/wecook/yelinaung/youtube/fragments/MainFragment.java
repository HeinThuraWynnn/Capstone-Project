package com.wecook.yelinaung.youtube.fragments;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.squareup.otto.Bus;
import com.wecook.yelinaung.Injection;
import com.wecook.yelinaung.R;
import com.wecook.yelinaung.database.DrinkDbModel;
import com.wecook.yelinaung.database.DrinksRepository;
import com.wecook.yelinaung.database.loaders.DrinksLoader;
import com.wecook.yelinaung.databinding.MainFragmentBinding;
import com.wecook.yelinaung.detail.DetailActivity;
import com.wecook.yelinaung.events.onBookmarkedEvent;
import com.wecook.yelinaung.font.CustomFont;
import com.wecook.yelinaung.youtube.adapters.MainRecyclerAdapter;
import com.wecook.yelinaung.youtube.scroll.EndlessRecyclerViewScrollListener;
import java.util.ArrayList;
import java.util.List;
import jp.wasabeef.recyclerview.animators.FadeInUpAnimator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by user on 5/10/16.
 */
public class MainFragment extends Fragment
    implements MainContract.View, SwipeRefreshLayout.OnRefreshListener,
    MainRecyclerAdapter.onItemEvent {
  LinearLayout linearLayout;
  TextView title;
  RecyclerView recyclerView;
  SwipeRefreshLayout swipeRefreshLayout;
  private MainContract.Presenter mPresenter;
  private DrinksRepository repository;
  private MainFragmentBinding mainFragmentBinding;
  private MainRecyclerAdapter adapter;
  private static MainFragment INSTANCE;
  private Bus bus = com.wecook.yelinaung.events.Bus.getInstance();

  public static MainFragment getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new MainFragment();
    }
    return INSTANCE;
  }

  @Override public void showDrinks(List<DrinkDbModel> list) {
    recyclerView.setVisibility(View.VISIBLE);
    linearLayout.setVisibility(View.GONE);
    title.setVisibility(View.GONE);
    adapter.noAnimationAddList(list);
  }

  @Override public void setLoadingIndicator(boolean active) {
    if (swipeRefreshLayout.isRefreshing() != active) {

      swipeRefreshLayout.setRefreshing(active);
    }
  }

  @Override public void onBookmark(View v, int position) {
    bus.post(new onBookmarkedEvent());
    mPresenter.processBookmarks(adapter.getItemAtPosition(position), position);
  }

  @Override public void onItemClick(View v, int position) {
    Intent intent = new Intent(getContext(), DetailActivity.class);
    intent.putExtra(DetailActivity.EXTRA, adapter.getItemAtPosition(position).getId());
    startActivity(intent);
  }

  @Override public void onLongPressed(View v, int position) {

  }

  @Override public void setPresenter(MainContract.Presenter presenter) {
    mPresenter = checkNotNull(presenter);
  }

  @Override public void showNoDrinks() {

  }

  @Override public void showDrinkDetail() {

  }

  @Override public void showError() {

  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
    adapter = new MainRecyclerAdapter(new ArrayList<DrinkDbModel>(0));
  }

  @Override public void onRefresh() {
    mPresenter.loadDrinks(true);
  }

  @Override public void showNoDrinksView() {
    recyclerView.setVisibility(View.GONE);
    linearLayout.setVisibility(View.VISIBLE);
    title.setVisibility(View.VISIBLE);
  }

  public void prepareSwipe(View rootView) {
    swipeRefreshLayout = mainFragmentBinding.mainSwipe;
    swipeRefreshLayout.setOnRefreshListener(this);
    swipeRefreshLayout.setColorSchemeResources(R.color.blue_light, R.color.color_red,
        R.color.colorAccent);
  }

  public void prepareRecycler(View rootView) {
    recyclerView = mainFragmentBinding.recycler;

    recyclerView.setItemAnimator(new FadeInUpAnimator());
    recyclerView.getItemAnimator().setAddDuration(300);
    recyclerView.getItemAnimator().setRemoveDuration(300);
    recyclerView.getItemAnimator().setMoveDuration(300);
    recyclerView.getItemAnimator().setChangeDuration(300);

    int columCount = getContext().getResources().getInteger(R.integer.recycler_item_count);
    GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), columCount);
    recyclerView.setLayoutManager(checkNotNull(gridLayoutManager));
    recyclerView.setOnScrollListener(new EndlessRecyclerViewScrollListener(gridLayoutManager) {

      @Override public void onLoadMore() {
        mPresenter.paginateDrinks();
      }
    });
    adapter.setItemEvent(this);
    recyclerView.setHasFixedSize(true);
    recyclerView.setAdapter(adapter);
  }

  private void showMessage(String message) {
    Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
  }

  @Nullable @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.main_fragment, container, false);
    mainFragmentBinding = DataBindingUtil.bind(rootView);
    linearLayout = mainFragmentBinding.errorContainer;
    title = mainFragmentBinding.title;
    ViewCompat.setElevation(mainFragmentBinding.errorCloud,
        getResources().getDimension(R.dimen.error_cloud_elevation));
    title.setTypeface(CustomFont.getInstance().getFont("medium"));
    prepareSwipe(rootView);
    prepareRecycler(rootView);
    repository = Injection.provideDrinkRepo(getActivity().getApplicationContext());
    DrinksLoader drinksLoader = new DrinksLoader(getContext(), repository);
    mPresenter = new MainPresenter(getLoaderManager(), repository, drinksLoader, this);
    swipeRefreshLayout.measure(rootView.getMeasuredWidth(), rootView.getMeasuredHeight());
    linearLayout.setOnClickListener((V) -> {
      if (!swipeRefreshLayout.isRefreshing()) {
        mPresenter.loadDrinks(true);
      }
    });

    return rootView;
  }

  @Override public void onAttach(Context context) {
    super.onAttach(context);
  }

  @Override public void onStart() {
    super.onStart();
  }

  @Override public void onResume() {
    super.onResume();
    bus.register(this);
    mPresenter.start();
  }

  @Override public void onPause() {
    bus.unregister(this);
    super.onPause();
  }

  @Override public void onDestroy() {
    mPresenter = null;
    super.onDestroy();
  }

  public interface OnFragmentInteractionListener {
    void onFragmentInteraction(Object object);
  }
}
