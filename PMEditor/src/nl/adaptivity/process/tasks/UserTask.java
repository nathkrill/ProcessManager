package nl.adaptivity.process.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.process.tasks.items.GenericItem;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;


public class UserTask {

  public static final String NS_TASKS = "http://adaptivity.nl/userMessageHandler";
  public static final String TAG_TASKS = "tasks";
  public static final String TAG_TASK = "task";
  public static final String TAG_ITEM = "item";
  public static final String TAG_OPTION = "option";
  private String mSummary;
  private long mHandle;
  private String mOwner;
  private String mState;
  private List<TaskItem> mItems;

  public UserTask(String pSummary, long pHandle, String pOwner, String pState, List<TaskItem> pItems) {
    mSummary = pSummary;
    mHandle = pHandle;
    mOwner = pOwner;
    mState = pState;
    mItems = pItems;
  }


  public String getSummary() {
    return mSummary;
  }


  public void setSummary(String pSummary) {
    mSummary = pSummary;
  }


  public long getHandle() {
    return mHandle;
  }


  public void setHandle(long pHandle) {
    mHandle = pHandle;
  }


  public String getOwner() {
    return mOwner;
  }


  public void setOwner(String pOwner) {
    mOwner = pOwner;
  }


  public String getState() {
    return mState;
  }


  public void setState(String pState) {
    mState = pState;
  }


  public List<TaskItem> getItems() {
    return mItems;
  }


  public void setItems(List<TaskItem> pItems) {
    mItems = pItems;
  }

  public static List<UserTask> parseTasks(InputStream pIn) throws XmlPullParserException, IOException {
    XmlPullParser in;
    try {
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(true);
      in = factory.newPullParser();
      in.setInput(pIn, "utf-8");
    } catch (Exception e){
      Log.e(UserTask.class.getName(), e.getMessage(), e);
      return null;
    }
    return parseTasks(in);
  }

  public static List<UserTask> parseTasks(XmlPullParser pIn) throws XmlPullParserException, IOException {
    if(pIn.getEventType()==XmlPullParser.START_DOCUMENT) {
      pIn.nextTag();
    }
    pIn.require(XmlPullParser.START_TAG, NS_TASKS, TAG_TASKS);
    ArrayList<UserTask> result = new ArrayList<>();
    while ((pIn.nextTag())==XmlPullParser.START_TAG) {
      result.add(parseTask(pIn));
    }
    pIn.require(XmlPullParser.END_TAG, NS_TASKS, TAG_TASKS);
    return result;
  }

  public static UserTask parseTask(XmlPullParser pIn) throws XmlPullParserException, IOException {
    pIn.require(XmlPullParser.START_TAG, NS_TASKS, TAG_TASK);
    String summary = pIn.getAttributeValue(null, "summary");
    long handle = Long.parseLong(pIn.getAttributeValue(null, "handle"));
    String owner = pIn.getAttributeValue(null, "owner");
    String state = pIn.getAttributeValue(null, "state");
    List<TaskItem> items = new ArrayList<>();
    while ((pIn.nextTag())==XmlPullParser.START_TAG) {
      items.add(parseTaskItem(pIn));
    }
    pIn.require(XmlPullParser.END_TAG, NS_TASKS, TAG_TASK);
    return new UserTask(summary, handle, owner, state, items);
  }

  public static TaskItem parseTaskItem(XmlPullParser pIn) throws XmlPullParserException, IOException {
    return parseTaskItemHelper(pIn, TaskItem.defaultFactory());
  }

  private static <T extends TaskItem> T parseTaskItemHelper(XmlPullParser pIn, TaskItem.Factory<T> pFactory) throws XmlPullParserException, IOException {
    pIn.require(XmlPullParser.START_TAG, NS_TASKS, TAG_ITEM);
    String name = pIn.getAttributeValue(null, "name");
    String label = pIn.getAttributeValue(null, "label");
    String type = pIn.getAttributeValue(null, "type");
    String value = pIn.getAttributeValue(null, "value");
    List<String> options = new ArrayList<>();
    while ((pIn.nextTag())==XmlPullParser.START_TAG) {
      pIn.require(XmlPullParser.START_TAG, NS_TASKS, TAG_OPTION);
      options.add(pIn.nextText());
      pIn.nextTag();
      pIn.require(XmlPullParser.END_TAG, NS_TASKS, TAG_OPTION);
    }
    pIn.require(XmlPullParser.END_TAG, NS_TASKS, TAG_ITEM);
    return pFactory.create(name, label, type, value, options);
  }

  public static GenericItem parseTaskGenericItem(XmlPullParser pIn) throws XmlPullParserException, IOException {
    return parseTaskItemHelper(pIn, TaskItem.genericFactory());
  }

}
