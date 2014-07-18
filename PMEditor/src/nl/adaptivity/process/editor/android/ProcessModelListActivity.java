package nl.adaptivity.process.editor.android;

import java.io.IOException;

import nl.adaptivity.android.darwin.AuthenticatedWebClient;
import nl.adaptivity.process.models.ProcessModelProvider;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;

/**
 * An activity representing a list of ProcessModels. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link ProcessModelDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ProcessModelListFragment} and the item details (if present) is a
 * {@link ProcessModelDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link ProcessModelListFragment.Callbacks} interface to listen for item
 * selections.
 */
public class ProcessModelListActivity extends Activity
    implements ProcessModelListFragment.Callbacks, ProcessModelDetailFragment.Callbacks {

  /**
   * Whether or not the activity is in two-pane mode, i.e. running on a tablet
   * device.
   */
  private boolean mTwoPane;

  private Account mAccount;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_processmodel_list);

    if (findViewById(R.id.processmodel_detail_container) != null) {
      // The detail container view will be present only in the
      // large-screen layouts (res/values-large and
      // res/values-sw600dp). If this view is present, then the
      // activity should be in two-pane mode.
      mTwoPane = true;

      // In two-pane mode, list items should be given the
      // 'activated' state when touched.
      ((ProcessModelListFragment) getFragmentManager()
          .findFragmentById(R.id.processmodel_list))
          .setActivateOnItemClick(true);
    }

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    String source = prefs.getString(SettingsActivity.PREF_SYNC_SOURCE, null);
    String authbase = AuthenticatedWebClient.getAuthBase(source);
    if (source!=null) {
      AsyncTask<String, Object, Account> task = new AsyncTask<String, Object, Account>() {

        @Override
        protected Account doInBackground(String... pParams) {
          mAccount = AuthenticatedWebClient.ensureAccount(ProcessModelListActivity.this, pParams[0]);
          ContentResolver.setIsSyncable(mAccount, ProcessModelProvider.AUTHORITY, 1);
          AccountManager accountManager = AccountManager.get(ProcessModelListActivity.this);
          AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {

            @Override
            public void run(AccountManagerFuture<Bundle> pFuture) {
              try {
                Bundle result = pFuture.getResult();
              } catch (OperationCanceledException | AuthenticatorException | IOException e) {
                Log.e(ProcessModelListActivity.class.getSimpleName(), "Failure to get auth token", e);
              }
              if (mAccount!=null) {
                requestSync(mAccount);
              }
            }};
          accountManager.getAuthToken(mAccount, AuthenticatedWebClient.ACCOUNT_TOKEN_TYPE, null, ProcessModelListActivity.this, callback  , null);

          return mAccount;
        }

        @Override
        protected void onPostExecute(Account account) {
        }

      };
      task.execute(authbase);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == android.R.id.home) {
      // This ID represents the Home or Up button. In the case of this
      // activity, the Up button is shown. Use NavUtils to allow users
      // to navigate up one level in the application structure. For
      // more details, see the Navigation pattern on Android Design:
      //
      // http://developer.android.com/design/patterns/navigation.html#up-vs-back
      //
      NavUtils.navigateUpFromSameTask(this);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * Callback method from {@link ProcessModelListFragment.Callbacks} indicating
   * that the item with the given ID was selected.
   */
  @Override
  public void onItemSelected(long pProcessModelRowId) {
    if (mTwoPane) {
      // In two-pane mode, show the detail view in this activity by
      // adding or replacing the detail fragment using a
      // fragment transaction.
      Bundle arguments = new Bundle();
      arguments.putLong(ProcessModelDetailFragment.ARG_ITEM_ID, pProcessModelRowId);
      ProcessModelDetailFragment fragment = new ProcessModelDetailFragment();
      fragment.setArguments(arguments);
      getFragmentManager().beginTransaction()
          .replace(R.id.processmodel_detail_container, fragment)
          .commit();

    } else {
      // In single-pane mode, simply start the detail activity
      // for the selected item ID.
      Intent detailIntent = new Intent(this, ProcessModelDetailActivity.class);
      detailIntent.putExtra(ProcessModelDetailFragment.ARG_ITEM_ID, pProcessModelRowId);
      startActivity(detailIntent);
    }
  }

  public void requestSync() {
    requestSync(mAccount);
  }

  public static void requestSync(Account account) {
    Bundle extras = new Bundle(1);
    extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
    ContentResolver.requestSync(account, ProcessModelProvider.AUTHORITY, extras );
  }

  public static void requestSync(Context pContext) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(pContext);
    String source = prefs.getString(SettingsActivity.PREF_SYNC_SOURCE, null);
    String authbase = AuthenticatedWebClient.getAuthBase(source);
    requestSync(AuthenticatedWebClient.ensureAccount(pContext, authbase));
  }
}
