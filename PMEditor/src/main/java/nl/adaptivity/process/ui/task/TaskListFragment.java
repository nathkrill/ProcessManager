/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.ui.task;

import android.annotation.TargetApi;
import android.content.SyncStatusObserver;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import nl.adaptivity.android.recyclerview.SelectableAdapter;
import nl.adaptivity.android.recyclerview.SelectableAdapter.OnSelectionListener;
import nl.adaptivity.android.util.MasterDetailOuterFragment;
import nl.adaptivity.android.util.MasterListFragment;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.tasks.data.TaskProvider;
import nl.adaptivity.process.ui.ProcessSyncManager;
import nl.adaptivity.process.ui.main.ListCursorLoaderCallbacks;
import nl.adaptivity.sync.SyncManager.SyncStatusObserverData;


/**
 * An activity representing a list of Tasks. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link TaskDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * Activities containing this fragment MUST implement the {@link ListCallbacks}
 * interface.
 */
public class TaskListFragment extends MasterListFragment<ProcessSyncManager> implements OnRefreshListener, OnSelectionListener {

  /**
   * The serialization (saved instance state) Bundle key representing the
   * activated item position. Only used on tablets.
   */
  private static final String STATE_ACTIVATED_ID = "activated_id";

  private static final int TASKLISTLOADERID = 3;

  private static final String TAG = TaskListFragment.class.getSimpleName();

  private TaskCursorAdapter mAdapter;
  private SwipeRefreshLayout mSwipeRefresh;
  private SyncStatusObserver mSyncObserver;
  private SyncStatusObserverData mSyncObserverHandle;
  private boolean mManualSync;
  private ListCursorLoaderCallbacks mTaskLoaderCallbacks;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public TaskListFragment() {}

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setHasOptionsMenu(true);
  }

  @Override
  public void onActivityCreated(final Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mAdapter = new TaskCursorAdapter(getActivity(), null);

    {
      final Bundle arguments = getArguments();
      if (arguments != null && arguments.containsKey(MasterDetailOuterFragment.ARG_ITEM_ID)) {
        final long itemId = arguments.getLong(MasterDetailOuterFragment.ARG_ITEM_ID);
        if (itemId>=0) { mAdapter.setSelectedItem(itemId); }
      }
    }

    mAdapter.setOnSelectionListener(this);
    setListAdapter(mAdapter);
    mTaskLoaderCallbacks = new TaskLoaderCallbacks(getActivity(), mAdapter);
    getLoaderManager().initLoader(TASKLISTLOADERID, null, mTaskLoaderCallbacks);
  }

  @Override
  public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
    return inflater.inflate(R.layout.refreshablerecyclerview, container, false);
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
  @Override
  public void onViewCreated(final View view, final Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    getRecyclerView().setLayoutManager(new LinearLayoutManager(getActivity()));
    mSwipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefresh);
    mSwipeRefresh.setOnRefreshListener(this);

    // Restore the previously serialized activated item position.
    if (savedInstanceState != null
        && savedInstanceState.containsKey(STATE_ACTIVATED_ID)) {
      setActivatedId(savedInstanceState.getLong(STATE_ACTIVATED_ID));
    }
    mSyncObserver = new SyncStatusObserver() {

      @Override
      public void onStatusChanged(final int which) {
        getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            updateSyncState();
          }
        });
      }
    };
  }

  @Override
  public void onResume() {
    super.onResume();
    mSyncObserver.onStatusChanged(0); // trigger status sync
//    mSyncObserverHandle = getCallbacks().getSyncManager().addOnStatusChangeObserver(TaskProvider.AUTHORITY,mSyncObserver);
  }

  @Override
  public void onPause() {
    super.onPause();
    if (mSyncObserverHandle!=null) {
      getCallbacks().getSyncManager().removeOnStatusChangeObserver(mSyncObserverHandle);
      mSyncObserverHandle=null;
    }
  }

  @Override
  public void onSaveInstanceState(final Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mAdapter != null && mAdapter.getSelectedPos() != RecyclerView.NO_POSITION) {
      // Serialise and persist the activated item position.
      outState.putLong(STATE_ACTIVATED_ID, mAdapter.getSelectedId());
    }
  }

  @Override
  public void onSelectionChanged(final SelectableAdapter adapter) {
    doOnItemSelected(adapter.getSelectedPos(), adapter.getSelectedId());
  }

  @Override
  public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
    inflater.inflate(R.menu.tasklist_menu, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    switch (item.getItemId()) {
      case R.id.ac_sync_tasks: {
        onRefresh();
        return true;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onRefresh() {
    getCallbacks().getSyncManager().requestSyncTaskList(true, ProcessSyncManager.DEFAULT_MIN_AGE);
    mManualSync=true;
    updateSyncState();
  }

  private void updateSyncState() {
    final ProcessSyncManager syncManager = getCallbacks().getSyncManager();
    if (syncManager!=null) {
        if (!syncManager.isSyncable(TaskProvider.AUTHORITY)) {
            mSwipeRefresh.setRefreshing(false);
        } else {
            final boolean syncActive  = syncManager.isTaskSyncActive();
            final boolean syncPending = syncManager.isTaskSyncPending();
            if (syncActive || (!syncPending)) { mManualSync = false; }
            final boolean sync = syncActive || mManualSync;
            mSwipeRefresh.setRefreshing(sync);
        }
    }
  }
}
