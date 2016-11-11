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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.darwin

import kotlinx.html.Tag
import kotlinx.html.TagConsumer
import kotlinx.html.div
import kotlinx.html.dom.create
import kotlinx.html.js.p
import kotlinx.html.js.span
import kotlinx.html.span
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.xhr.FormData
import org.w3c.xhr.XMLHttpRequest
import uk.ac.bournemouth.darwin.JSServiceContext
import uk.ac.bournemouth.darwin.LoginDialog
import uk.ac.bournemouth.darwin.accountsLoc
import uk.ac.bournemouth.darwin.sharedhtml.*
import uk.ac.bournemouth.darwin.util.*
import java.io.Closeable
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.*

object html {
  val context: JSServiceContext = JSServiceContext()

  val shared:shared = uk.ac.bournemouth.darwin.sharedhtml.shared

  @JsName("onLinkClick")
  fun onLinkClick(event: MouseEvent) {
    if (event.button == BUTTON_DEFAULT) {
      val target = event.target as? HTMLAnchorElement
      var href = target?.pathname
      // handle urls to virtual pages
      if (href != null) {
        if (href == "/") {
          href = target?.hash ?: "/"
        }
        navigateTo(href, true, true)
        event.preventDefault()
        event.stopPropagation()
      }
    }

  }

  @JsName("error")
  fun error(message: String, exception: Throwable? = null) {
    console.error(message, exception)
    val completeMessage: String
    if (exception == null) {
      completeMessage = message
    } else {
      completeMessage = message + "<br />" + exception.message
    }
    modalDialog(completeMessage)
  }

  fun <T> appendContent(content: ContextTagConsumer<*>.() -> T):T {
    return mContentPanel!!.appendHtml.withContext(html.context).content()
  }

  @JsName("navigateTo")
  fun navigateTo(locationParam: String?, addHistory: Boolean, doRedirect: Boolean) = navigateToImpl(locationParam, addHistory, doRedirect)

  @JsName("setContent")
  fun setContent(block : TagConsumer<HTMLElement>.() -> Unit) {
    val contentPanel = mContentPanel!!
    contentPanel.clear()
    contentPanel.appendHtml(block)
  }

  val contentPanel:HTMLElement? get() = mContentPanel

}

private fun onMenuReceived(request: XMLHttpRequest) {
  if (request.status in 200..399) {
    val holder = document.createElement("holder")
    holder.innerHTML = request.responseText
    val m = menu
    m.clear()
    var first = true
    for (child in holder.firstElementChild?.childElements()) {
      if (child !is Text) {
        if (first) first = false else m.appendChild(document.createTextNode("\n"))
        m.appendChild(child)
      }
    }
    convertMenuToJS()
  } else {
    html.error("Error updating the menu: ${request.statusText} (${request.status})")
  }
}

private fun onContentPanelReceived(request: XMLHttpRequest, location: String) {
  val statusCode: Int = request.status.toInt()
  if (statusCode == 401) {
    hideBanner()
    loginDialog() // just return
    return
  } else if (statusCode !in 200..399) {
    hideBanner()
    html.error("Failure to load panel: " + statusCode)
    return
  }
  mContentPanel!!.clear()
  hideBanner()
  val root:Element? = request.responseXML?.documentElement
  if (root != null) {
    var windowtitle: String? = null
    var pagetitle: String = "Darwin"
    var body: String = ""
    root.childNodes.forEach { childNode ->
      if (childNode is Element) {
        val childElement = childNode
        when (childElement.nodeName) {
          "title" -> if (windowtitle == null) {
            windowtitle = childElement.getAttribute("windowtitle") ?: childElement.textContent
            pagetitle = childElement.innerHTML
          }
          "body"  -> if (body.size==0) {
            body = childElement.innerHTML
          } else html.error("unexpected child in dynamic content: ${childElement.nodeName}")
        }
      }
    }

    windowtitle?.let { document.title = it }
    window.history.pushState(data = location, title = "location", url = location)

    val pageTitleElement = document.getElementById("title")
    if (pagetitle==null) {
      pageTitleElement?.textContent = "Darwin"
    } else {
      pageTitleElement?.let { it.innerHTML = pagetitle }
    }

    mContentPanel?.let { it.innerHTML = body }
  }
}

private fun onContentPanelError(request: XMLHttpRequest) {
  hideBanner()
  html.error("The requested location is not available: ${request.statusText} (${request.status})")
}

//  interface DarwinUiBinder extends UiBinder<Widget, Darwin> { /* Dynamic gwt */}

private val menu: HTMLElement
  get() {
    return document.getElementById("menu") as HTMLElement
  }

private fun onLoginResult(request: XMLHttpRequest) {
  val text = request.responseText
  val cpos = text.indexOf(':')

  val eolpos:Int = run {
    val lfpos = text.indexOf('\n', cpos)
    val crpos = text.indexOf('\r', cpos)
    if (lfpos >= 0) {
      if (crpos>=0 && crpos<lfpos) crpos else lfpos
    } else crpos
  }
  val result: String
  val payload: String?
  if (cpos >= 0) {
    result = text.substring(0, cpos)
    if (eolpos >= 0) {
      payload = text.substring(cpos + 1, eolpos)
    } else {
      payload = text.substring(cpos + 1)
    }
  } else {
    result = if (eolpos>0) text.substring(0, eolpos) else text
    payload = null
  }

  if ("login" == result && payload != null) {
    mLoggedInUser = payload
    closeDialogs()
    updateLoginPanel()
    requestRefreshMenu(mLocation!!)
  } else if ("logout" == result) {
    mLoggedInUser = null
    closeDialogs()
    updateLoginPanel()
    requestRefreshMenu(mLocation!!)
    html.navigateTo("/", true, true)
  } else if ("error" == result) {
    closeDialogs()
    html.error("Error validating login: " + payload!!, null)
  } else if ("invalid" == result) {
    val dialog = mLoginDialog
    if (dialog ==null) {
      html.error("Login dialog missing")
    } else {

      dialog.errorMsg = "Credentials invalid"
      dialog.password = null
    }
  } else {
    closeDialogs()
    html.error("Invalid response received from login form (${text}) : ${request.statusText} (${request.status})", null)
  }
}

private fun onLoginError(request: XMLHttpRequest) {
  html.error("Could not login due to request error: ${request.statusText} (${request.status})")
  closeDialogs()
}

private fun onLoginDialogConfirm(event: Event) {
  val form = (event.target!! as HTMLInputElement).form!!

  event.stopPropagation()

  val username: String? = (form.get("username") as? HTMLInputElement)?.value
  val password: String? = (form.get("password") as? HTMLInputElement)?.value

  if (username.isNullOrBlank()) {
    /* set error bit*/
  }
  if (password.isNullOrBlank()) {
    /* set error bit */
  }

  val request = XMLHttpRequest().apply {
    open("POST", "${html.context.accountMgrPath}${LOGIN_LOCATION}")
    setRequestHeader("Accept", "text/plain")
    onload = { onLoginResult(this) }
    onerror = { onLoginError(this) }
  }

  val postData = FormData().apply {
    append("username", username)
    append("password", password)
  }

  try {
    request.send(postData)
  } catch (e: Exception) {
    html.error("Could not send login request", e)
    closeDialogs()
  }
  event.preventDefault()
  event.stopPropagation()
}

private fun dialogCloseHandler(event: Event) = closeDialogs(event)

private fun onLoginOutClicked(event: MouseEvent) {
  val username = document.getElementById("username")?.textContent
  if (username.isNullOrEmpty()) {
    loginDialog()
    // Login
  } else {
    val request = XMLHttpRequest().apply {
      open("GET", "${accountsLoc}logout")
      setRequestHeader("Accept", "application/binary")
      onload = { onLoginResult(this) }
      onerror = { html.error("Error logging out: ${statusText} (${status})") }
    }
    try {
      request.send()
    } catch (e: Exception) {
      html.error("Could not log out", e)
    }

  }
}

/*
private inner class HistoryChangeHandler : ValueChangeHandler<String> {

  fun onValueChange(event: ValueChangeEvent<String>) {
    val newValue = event.getValue()
    navigateTo(newValue, false, false)
  }

}
*/

private fun onLinkClick(event: MouseEvent) {
  return html.onLinkClick(event)
}

private var mLocation: String? = null

private var mLoginDialog: LoginDialog? = null

private var mUsernameFromManager: String? = null
private var mPasswordFromManager: String? = null

private var mLoggedInUser: String? = document.getElementById("username")?.textContent

private var mContentPanel: HTMLElement? = null

internal var dialogTitle: HTMLSpanElement? = null

private var mLoginoutRegistration: Closeable? = null

private var mUsernameRegistration: Closeable? = null

private var mBanner: Element? = null

fun main(args: Array<String>) {
  val newLocation = window.location.hash.let { if (it.isNullOrBlank()) window.location.pathname else it }

  (document.getElementById("xloginform") as? HTMLFormElement)?.let { form ->
    mUsernameFromManager = (form["username"] as? HTMLInputElement)?.value
    mPasswordFromManager = (form["password"] as? HTMLInputElement)?.value
    form.removeFromParent() // No longer needed
  }

  mContentPanel = document.getElementById("content") as HTMLElement

  if (!window.location.hash.isNullOrBlank()) {
    requestRefreshMenu(newLocation)
  }

  convertMenuToJS()

  registerLoginPanel()

  //    History.addValueChangeHandler(HistoryChangeHandler())

  mBanner = document.getElementById("banner")

  // This is not a page that already has it's content.
  if (asInlineLocation(newLocation) == null) {
    showBanner()
    html.navigateTo(newLocation, false, false)
  } else {
    mLocation = newLocation
  }
}

/**
 * @category ui_elements
 */
private fun hideBanner() = mBanner?.setAttribute("style", "display:none")

/**
 * @category ui_elements
 */
private fun showBanner() = mBanner?.removeAttribute("style")

class JSContextTagConsumer<T>(context: JSServiceContext, myDelegate: TagConsumer<T>) : ContextTagConsumer<T>(html.context, myDelegate) {
  fun darwinDialog(title: String, id: String? = null, positiveButton:JSButton?=JSButton("Ok","btn_dlg_ok", ::dialogCloseHandler), negativeButton:JSButton?=null, vararg otherButtons:JSButton, bodyContent: ContextTagConsumer<*>.() -> Unit = {}):Node {
    val dialog = document.create.withContext(context).darwinDialog(title, id, positiveButton, negativeButton, *otherButtons, bodyContent = bodyContent)
    val buttons = (listOf(positiveButton, negativeButton).asSequence() + listOf(*otherButtons).asSequence())
          .filterNotNull()
          .associateBy { it.id }
    dialog.visitDescendants { descendant ->
      if (descendant is HTMLElement) {
        descendant.id?.let {
          buttons[it]
        }?.let { button ->
          descendant.onclick = button.handler
        }
      }
    }

    return dialog
  }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T,C:TagConsumer<out T>> C.withContext(context:JSServiceContext) = JSContextTagConsumer<T>(context, this)

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
inline fun <T: Tag> T.withContext(context:JSServiceContext) = (consumer as TagConsumer<out T>).withContext(context)

class JSButton(label:String, id:String, val handler: (Event)->dynamic):SharedButton(label, id) {}

/**
 * @category ui_elements
 */
private fun modalDialog(string: String) {
  val okButton = JSButton("Ok", "btn_modal_ok", {dialogCloseHandler(it)})
  html.appendContent {
    darwinDialog("message", positiveButton = okButton) {
      div {
        span { +string }
      }
    }
  }
}

private fun loginDialog() {
  val setConfirmFunction: (HTMLElement) -> Unit = { it.onclick = ::onLoginDialogConfirm }

  val setCancelFunction: (HTMLElement) -> Unit = { it.onclick = ::closeDialogs }

  val loginDialog = LoginDialog(html.context, username = mUsernameFromManager, password = mPasswordFromManager, visitConfirm = setConfirmFunction, visitCancel = setCancelFunction)
  mLoginDialog = loginDialog

  mContentPanel!!.appendChild(loginDialog.element)
}

/**
 * @category ui_elements
 */
private fun updateDialogTitle(string: String) {
  dialogTitle?.textContent = string
}

/**
 * @category ui_elements
 */
private fun dialog(title: String, id: String? = null, content: ContextTagConsumer<*>.() -> Unit) {
  html.appendContent { darwinDialog(title = title, id = id, bodyContent = content) }
}

private fun closeDialogs(event: dynamic = null) {
  val contentPanel = mContentPanel
  if (contentPanel != null) {
    contentPanel.removeChildElementIf { it.hasClass("dialog") }
  }
  if (mLoginDialog != null) {
    mLoginDialog = null
  }
  if (event!=null) {
    event.preventDefault()
    event.stopPropagation()
  }
}

private fun navigateToImpl(locationParam: String?, addHistory: Boolean, doRedirect: Boolean) {
  var location = mLocation
  var effectiveAddHistory = addHistory
  val newLocation = locationParam?.let { if (it[0] == '#') it.substring(1) else it } ?: "/"

  if (location == null && newLocation != null || location != null && location != newLocation) {
    if (location != null && location.startsWith("${accountsLoc}myaccount")) {
      location = newLocation
      updateLoginPanel()
    } else {
      location = newLocation
    }

    if (location == "/" || location == "" || location == null) {
      hideBanner()
      setInboxPanel()
      location = "/"
    } else if (location == "/actions") {
      hideBanner()
      setActionPanel()
      location = "/#/actions"
    } else if (location == "/processes") {
      hideBanner()
      setProcessesPanel()
      location = "#/processes"
    } else if (location == "/about") {
      hideBanner()
      setAboutPanel()
      location = "#/about"
    } else {
      val inlineLocation = asInlineLocation(location)
      if (inlineLocation != null) {
        location = inlineLocation
        val contentPanel = mContentPanel
        if (contentPanel != null) {
          contentPanel.clear()
          contentPanel.appendHtml.span("label") { +"Loading" }
        }

        val request = XMLHttpRequest().apply {
          open("GET", inlineLocation)
          setRequestHeader("Accept", "text/html")
          setRequestHeader("X-Darwin", "nochrome")
        }
        effectiveAddHistory = false // don't add history here.
        request.onload = { onContentPanelReceived(request, inlineLocation) }
        request.onerror = { onContentPanelError(request) }
        try {
          request.send()
        } catch (e: Exception) {
          html.error("Could load requested content", e)
          closeDialogs()
        }

      } else {
        if (doRedirect) {
          // Load the page
          window.location.assign(newLocation)
        } else {
          hideBanner()
        }
      }
    }
    if (effectiveAddHistory) {
      window.history.pushState(data = location, title = "location", url = location)
      //        History.newItem(location, false)
    }
    mLocation = location
    updateMenuTabs()
  }
}

private fun updateMenuTabs() {
  for (menuitem in menu.childElements()) {
    if (menuitem is HTMLAnchorElement)
      updateLinkItem(menuitem)
  }
}

private fun updateLinkItem(menuitem: HTMLAnchorElement) {
  var href = menuitem.pathname
  if (href != null && href.length > 0) {
    if (href == "/" && menuitem.hash.length > 1) {
      href = menuitem.hash.substring(1) // skip # character
    }
    val loc = mLocation?.let {
      if (it.startsWith("/#")) it.substring(2) else if (it.startsWith("#")) it.substring(1) else it
    }
    if (href == loc) {
      menuitem.addClass("active")
    } else {
      menuitem.removeClass("active")
    }

  }
}

/**
 * Make the menu elements active and add an onClick Listener.
 */
private fun convertMenuToJS() {
  // TODO convert this into a window handler that is a bit smarter.
  for (item in menu.elements()) {
    if (item is HTMLAnchorElement) {
      item.onClick(true, { event -> html.onLinkClick(event) })
      updateLinkItem(item)
    }
  }
}

private fun setContentOld(vararg newContent: Node) {
  val contentPanel = mContentPanel!!
  contentPanel.clear()
  for (node in newContent) {
    contentPanel.appendChild(node)
  }
}

private fun setContentOld(newContent: NodeList) {
  val contentPanel = mContentPanel!!
  for (node in newContent) {
    contentPanel.appendChild(node)
  }
}

private inline fun setContentOld(block: () -> Node) {
  setContentOld(block())
}

private inline fun setContentListOld(block: () -> NodeList) {
  setContentOld(block())
}

private fun setInboxPanel() {
  html.setContent {
    span { +"Inbox panel - work in progress" }
  }
}

private fun setProcessesPanel() {
  html.setContent { span {+"Processes panel - work in progress" }}
}

private fun setActionPanel() {
  html.setContent { plus("Action panel - work in progress") }
}

private fun setAboutPanel() {
  html.setContent {
    document.create.p {
      +"""Welcome to the Darwin server. This server functions as a research prototype as well
          as support for the Web Information Systems and Application Programming units."""
    }
  }
}


private fun registerLoginPanel() {
  mLoginoutRegistration = document.getElementById("logout")?.let { it.removeAttribute("href"); it.onClick { ev -> onLoginOutClicked(ev) } }
  mUsernameRegistration = document.getElementById("username")?.let { /*it.removeAttribute("href");*/ it.onClick { ev -> html.onLinkClick(ev) } }
}

private fun unregisterLoginPanel() {
  mLoginoutRegistration?.close()
  mLoginoutRegistration = null
  mUsernameRegistration?.close()
  mUsernameRegistration = null
}


private fun updateLoginPanel() {
  unregisterLoginPanel()

  val loginPanel = document.getElementById("login") as HTMLDivElement
  loginPanel.clear()
  loginPanel.appendHtml.loginPanelContent(html.context, mLoggedInUser)
  registerLoginPanel()
}


private fun requestRefreshMenu(location: String) {
  val request = XMLHttpRequest().apply {
    open("GET", "/common/menu?location=${encodeURI(location)}")
    onload = { onMenuReceived(this) }
    onerror = { html.error("Could not update menu: ${statusText} ($status)") }
  }
  try {
    request.send()
  } catch (e: Exception) {
    log("Could not update menu", e)
  }

}

private val LOGIN_LOCATION = "login"

private val INLINEPREFIXES = arrayOf("${accountsLoc}chpasswd", "${accountsLoc}myaccount")

private fun asInlineLocation(location: String): String? {
  for (prefix in INLINEPREFIXES) {
    if (location.startsWith(prefix)) {
      return prefix
    }
  }
  return null
}

private fun log(message: String, throwable: Throwable) {
  console.warn(message, throwable)
}

private fun log(message: String) {
  console.info(message)
}
