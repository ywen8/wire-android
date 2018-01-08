/**
  * Wire
  * Copyright (C) 2018 Wire Swiss GmbH
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
package com.waz.zclient.usersearch

import android.Manifest.permission.READ_CONTACTS
import android.content.{DialogInterface, Intent}
import android.net.Uri
import android.os.{Bundle, Handler}
import android.support.design.widget.TabLayout
import android.support.design.widget.TabLayout.OnTabSelectedListener
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.{LinearLayoutManager, RecyclerView, Toolbar}
import android.view._
import android.view.animation.Animation
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import android.widget._
import com.waz.ZLog.ImplicitTag._
import com.waz.ZLog._
import com.waz.content.UserPreferences
import com.waz.model.ConversationData.ConversationType
import com.waz.model.UserData.ConnectionStatus
import com.waz.model._
import com.waz.permissions.PermissionsService
import com.waz.service.{NetworkModeService, SearchState, ZMessaging}
import com.waz.threading.Threading
import com.waz.utils.events.Signal
import com.waz.utils.returning
import com.waz.utils.wrappers.AndroidURIUtil
import com.waz.zclient._
import com.waz.zclient.common.controllers._
import com.waz.zclient.common.controllers.global.AccentColorController
import com.waz.zclient.common.views.{ChatheadWithTextFooter, FlatWireButton, PickableElement}
import com.waz.zclient.controllers.globallayout.KeyboardVisibilityObserver
import com.waz.zclient.controllers.navigation.Page
import com.waz.zclient.controllers.userpreferences.IUserPreferencesController
import com.waz.zclient.conversation.ConversationController
import com.waz.zclient.core.stores.conversation.ConversationChangeRequester
import com.waz.zclient.integrations.{IntegrationDetailsController, IntegrationDetailsFragment}
import com.waz.zclient.pages.BaseFragment
import com.waz.zclient.pages.main.conversation.controller.IConversationScreenController
import com.waz.zclient.pages.main.participants.dialog.DialogLaunchMode
import com.waz.zclient.pages.main.pickuser.controller.IPickUserController
import com.waz.zclient.ui.animation.fragment.FadeAnimation
import com.waz.zclient.ui.startui.{ConversationQuickMenu, ConversationQuickMenuCallback}
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.ui.theme.ThemeUtils
import com.waz.zclient.ui.utils.KeyboardUtils
import com.waz.zclient.usersearch.ContactsController.{ContactDetails, ContactMethod}
import com.waz.zclient.usersearch.adapters.PickUsersAdapter
import com.waz.zclient.usersearch.views.{ContactRowView, SearchBoxView, SearchEditText, UserRowView}
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.utils.device.DeviceDetector
import com.waz.zclient.utils.{IntentUtils, LayoutSpec, RichView, StringUtils, UiStorage, UserSignal, ViewUtils}
import com.waz.zclient.views._

import scala.collection.JavaConverters._
import scala.concurrent.Future

object PickUserFragment {
  val TAG: String = classOf[PickUserFragment].getName
  val ARGUMENT_ADD_TO_CONVERSATION: String = "ARGUMENT_ADD_TO_CONVERSATION"
  val ARGUMENT_GROUP_CONVERSATION: String = "ARGUMENT_GROUP_CONVERSATION"
  val ARGUMENT_CONVERSATION_ID: String = "ARGUMENT_CONVERSATION_ID"
  val NUM_SEARCH_RESULTS_LIST: Int = 30
  val NUM_SEARCH_RESULTS_TOP_USERS: Int = 24
  val NUM_SEARCH_RESULTS_ADD_TO_CONV: Int = 1000
  private val DEFAULT_SELECTED_INVITE_METHOD: Int = 0
  private val SHOW_KEYBOARD_THRESHOLD: Int = 10

  def newInstance(addToConversation: Boolean, conversationId: String): PickUserFragment = {
    newInstance(addToConversation, groupConversation = false, conversationId)
  }

  def newInstance(addToConversation: Boolean, groupConversation: Boolean, conversationId: String): PickUserFragment = {
    val fragment: PickUserFragment = new PickUserFragment
    val args: Bundle = new Bundle
    args.putBoolean(ARGUMENT_ADD_TO_CONVERSATION, addToConversation)
    args.putString(ARGUMENT_CONVERSATION_ID, conversationId)
    args.putBoolean(ARGUMENT_GROUP_CONVERSATION, groupConversation)
    fragment.setArguments(args)
    fragment
  }

  trait Container {
    def showIncomingPendingConnectRequest(conv: ConvId): Unit

    def onSelectedUsers(users: java.util.List[UserId], requester: ConversationChangeRequester): Unit

    def getLoadingViewIndicator: LoadingIndicatorView

    def getCurrentPickerDestination: IPickUserController.Destination
  }

}

class PickUserFragment extends BaseFragment[PickUserFragment.Container]
  with FragmentHelper
  with View.OnClickListener
  with KeyboardVisibilityObserver
  with ConversationQuickMenuCallback
  with OnBackPressedListener
  with SearchResultOnItemTouchListener.Callback
  with PickUsersAdapter.Callback {

  private lazy val searchResultAdapter: PickUsersAdapter = new PickUsersAdapter(
    new SearchResultOnItemTouchListener(getActivity, this),
    this,
    searchUserController,
    integrationsController,
    themeController.isDarkTheme || !isAddingToConversation
  )
  // Saves user from which a pending connect request is loaded
  private var isKeyboardVisible: Boolean = false
  private var searchBoxIsEmpty: Boolean = true
  private var showLoadingBarDelay: Long = 0L
  private var lastInputIsKeyboardDoneAction: Boolean = false
  private lazy val searchUserController: SearchUserController = new SearchUserController(SearchState("", hasSelectedUsers = false, addingToConversation = addingToConversation))

  private lazy val searchResultRecyclerView        = view[RecyclerView](R.id.rv__pickuser__header_list_view)
  private lazy val conversationToolbar             = view[Toolbar](R.id.t_pickuser_toolbar)
  private lazy val startUiToolbar                  = view[Toolbar](R.id.pickuser_toolbar)
  private lazy val toolbarHeader                   = view[TextView](R.id.ttv__pickuser__add_header)
  private lazy val errorMessageView                = view[TypefaceTextView](R.id.pickuser__error_text)
  private lazy val conversationQuickMenu           = view[ConversationQuickMenu](R.id.cqm__pickuser__quick_menu)
  private lazy val userSelectionConfirmationButton = view[FlatWireButton](R.id.confirmation_button)
  private lazy val inviteButton                    = view[FlatWireButton](R.id.invite_button)
  private lazy val searchBoxView                   = view[SearchEditText](R.id.sbv__search_box)
  private lazy val toolbarTitle                    = view[TypefaceTextView](R.id.pickuser_title)

  private var teamPermissions = Set[AccountData.Permission]()

  private implicit lazy val uiStorage = inject[UiStorage]
  private implicit lazy val context   = getContext

  private lazy val zms                    = inject[Signal[ZMessaging]]
  private lazy val self                   = zms.flatMap(z => UserSignal(z.selfUserId))
  private lazy val userAccountsController = inject[UserAccountsController]
  private lazy val accentColor            = inject[AccentColorController].accentColor.map(_.getColor())
  private lazy val themeController        = inject[ThemeController]
  private lazy val conversationController = inject[ConversationController]
  private lazy val browser                = inject[BrowserController]
  private lazy val integrationsController = inject[IntegrationsController]
  private lazy val pickUserController     = inject[IPickUserController]
  private lazy val convScreenController   = inject[IConversationScreenController]
  private lazy val contactsController     = inject[ContactsController]

  private lazy val emptyListButton = returning(view[RelativeLayout](R.id.empty_list_button)){ v =>
    (for {
      zms <- zms
      permissions <- userAccountsController.permissions.orElse(Signal.const(Set.empty[AccountData.Permission]))
      members <- zms.teams.searchTeamMembers().orElse(Signal.const(Set.empty[UserData]))
      searching <- Option(searchUserController).fold(Signal.const(false))(_.searchState.map(!_.empty))
    } yield
      if (zms.teamId.nonEmpty && permissions(AccountData.Permission.AddTeamMember) && !members.exists(_.id != zms.selfUserId) && !isAddingToConversation && !searching)
        View.VISIBLE
      else
        View.GONE)
      .onUi(v.setVisibility(_))

    v.foreach(_.onClick(browser.openUrl(AndroidURIUtil.parse(getString(R.string.pick_user_manage_team_url)))))
  }

  private case class PickableUser(userId : UserId, userName: String) extends PickableElement {
    def id: String = userId.str
    def name: String = userName
  }

  private object PickableUser {
    def apply(userData: UserData): PickableUser = new PickableUser(userData.id, userData.getDisplayName)
  }

  final val searchBoxViewCallback: SearchBoxView.Callback = new SearchBoxView.Callback() {
    override def onRemovedTokenSpan(element: PickableElement): Unit = {
      searchUserController.removeUser(UserId(element.id))
      if (isAddingToConversation) {
        setConversationQuickMenuVisible(false)
      }
    }

    override def onFocusChange(hasFocus: Boolean): Unit = {}

    override def onClearButton(): Unit = closeStartUI()

    override def afterTextChanged(s: String): Unit = {
      val filter = searchBoxView.getSearchFilter
      searchUserController.setFilter(filter)
      integrationsController.searchQuery ! filter
      onSearchBoxHasNewSearchFilter(filter)
      if (filter.isEmpty && searchBoxView.getElements.isEmpty)
        onSearchBoxIsEmpty()
    }

  }

  override def onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation = {
    if (nextAnim == 0 || getContainer == null || getControllerFactory.isTornDown)
      super.onCreateAnimation(transit, enter, nextAnim)
    else if (pickUserController.isHideWithoutAnimations)
      new DefaultPageTransitionAnimation(0, getOrientationIndependentDisplayHeight(getActivity), enter, 0, 0, 1f)
    else if (enter)  // Fade animation in participants dialog on tablet
      if (LayoutSpec.isTablet(getActivity) && isAddingToConversation)
        new FadeAnimation(getInt(R.integer.open_new_conversation__top_conversation__animation_duration), 0f, 1f)
      else
        new DefaultPageTransitionAnimation(0,
          getDimenPx(R.dimen.open_new_conversation__thread_list__max_top_distance),
          enter,
          getInt(R.integer.framework_animation_duration_long),
          getInt(R.integer.framework_animation_duration_medium),
          1f)
    else
      new DefaultPageTransitionAnimation(
        0,
        getDimenPx(R.dimen.open_new_conversation__thread_list__max_top_distance),
        enter,
        getInt(R.integer.framework_animation_duration_medium),
        0,
        1f)

  }

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
  }

  override def onCreateView(inflater: LayoutInflater, viewContainer: ViewGroup, savedInstanceState: Bundle): View =
    inflater.inflate(R.layout.fragment_pick_user, viewContainer, false)

  override def onViewCreated(rootView: View, savedInstanceState: Bundle): Unit = {
    conversationToolbar.setNavigationOnClickListener(new View.OnClickListener() {
      def onClick(v: View): Unit = {
        closeStartUI()
      }
    })

    searchResultRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity))
    searchResultRecyclerView.setAdapter(searchResultAdapter)
    searchResultRecyclerView.addOnItemTouchListener(new SearchResultOnItemTouchListener(getActivity, this))
    if (isAddingToConversation) {
      searchResultRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
        override def onScrollStateChanged(recyclerView: RecyclerView, newState: Int): Unit = {
          if (newState == RecyclerView.SCROLL_STATE_DRAGGING && getControllerFactory.getGlobalLayoutController.isKeyboardVisible) {
            KeyboardUtils.hideKeyboard(getActivity)
          }
        }
      })
    }

    searchBoxView.setCallback(searchBoxViewCallback)
    conversationQuickMenu.setCallback(this)
    conversationQuickMenu.setVisibility(View.GONE)
    userSelectionConfirmationButton.setGlyph(R.string.glyph__add_people)
    userSelectionConfirmationButton.setVisibility(View.GONE)
    inviteButton.setText(R.string.pref_invite_title)
    inviteButton.setGlyph(R.string.glyph__invite)
    // Error message
    errorMessageView.setVisibility(View.GONE)
    showLoadingBarDelay = getResources.getInteger(R.integer.people_picker__loading_bar__show_delay)
    if (isAddingToConversation) {
      inviteButton.setVisibility(View.GONE)
      conversationToolbar.setVisibility(View.VISIBLE)
      startUiToolbar.setVisibility(View.GONE)
      toolbarHeader.setText(if (isGroupConversation) getString(R.string.people_picker__toolbar_header__group)
      else getString(R.string.people_picker__toolbar_header__one_to_one))
      userSelectionConfirmationButton.setText(if (isGroupConversation) R.string.people_picker__confirm_button_title__add_to_conversation
      else R.string.people_picker__confirm_button_title__create_conversation)
      ViewUtils.setHeight(searchBoxView, getResources.getDimensionPixelSize(R.dimen.searchbox__height__with_toolbar))
      searchBoxView.applyDarkTheme(ThemeUtils.isDarkTheme(getContext))
    } else {
      inviteButton.setVisibility(if (isPrivateAccount) View.VISIBLE else View.GONE)
      // Use constant style for left side start ui
      val textColor: Int = ContextCompat.getColor(getContext, R.color.text__primary_dark)
      conversationToolbar.setVisibility(View.GONE)
      startUiToolbar.setVisibility(View.VISIBLE)
      searchBoxView.applyDarkTheme(true)
      startUiToolbar.inflateMenu(R.menu.toolbar_close_white)
      startUiToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
        override def onMenuItemClick(item: MenuItem): Boolean = {
          item.getItemId match {
            case R.id.close =>
              closeStartUI()
          }
          false
        }
      })
      if (isPrivateAccount)
        userAccountsController.currentUser.on(Threading.Ui) {
          case Some(userData) =>
            toolbarTitle.setText(userData.getDisplayName)
          case _ =>
        }
      else
        (for {
          z <- zms
          t <- z.teams.selfTeam
        } yield t.map(_.name).getOrElse("")).onUi {
          toolbarTitle.setText
        }
    }

    accentColor.on(Threading.Ui) { color =>
      conversationQuickMenu.setAccentColor(color)
      searchBoxView.setCursorColor(color)
    }

    searchUserController.onSelectedUserAdded.on(Threading.Ui) { onSelectedUserAdded }

    searchUserController.onSelectedUserRemoved.on(Threading.Ui) { userId =>
      onSelectedUserRemoved(userId)
      if (getSelectedUsers.isEmpty)
        onSearchBoxIsEmpty()
    }

    searchBoxView.setOnEditorActionListener(new OnEditorActionListener {
      override def onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean = {
        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
          onKeyboardDoneAction()
          true
        } else false
      }
    })

    for {
      z <- zms
      accountData <- z.account.accountData
    } {
      teamPermissions = accountData.selfPermissions
    }

    val tabs = findById[TabLayout](rootView, R.id.pick_user_tabs)
    import Threading.Implicits.Ui
    searchResultAdapter.peopleOrServices.head.map {
      case false => 0
      case true  => 1
    }.foreach(tabs.getTabAt(_).select())

    tabs.addOnTabSelectedListener(new OnTabSelectedListener {
      override def onTabSelected(tab: TabLayout.Tab): Unit = {
        tab.getPosition match {
          case 0 => searchResultAdapter.peopleOrServices ! false
          case 1 => searchResultAdapter.peopleOrServices ! true
        }
        searchBoxView.removeAllElements()
      }
      override def onTabUnselected(tab: TabLayout.Tab): Unit = {
      }
      override def onTabReselected(tab: TabLayout.Tab): Unit = {}
    })

    val internalVersion = BuildConfig.APPLICATION_ID match {
      case "com.wire.internal" | "com.waz.zclient.dev" | "com.wire.x" | "com.wire.qa" => true
      case _ => false
    }

    val shouldShowTabs = for {
      zms <- zms.head
      conv <- addingToConversation.fold(Future.successful(Option.empty[ConversationData]))(zms.convsStorage.get)
      members <- addingToConversation.fold(Future.successful(Option.empty[Int]))(cId => zms.membersStorage.getActiveUsers(cId).map(m => Some(m.size)))
    } yield zms.teamId.nonEmpty && internalVersion && conv.forall(_.convType == ConversationType.Group) && members.forall(_ > 2)

    shouldShowTabs.map {
      case true => tabs.setVisibility(View.VISIBLE)
      case _ => tabs.setVisibility(View.GONE)
    } (Threading.Ui)

    if (isAddingToConversation && !themeController.isDarkTheme) {
      tabs.setSelectedTabIndicatorColor(getColor(R.color.light_graphite))
      (0 until tabs.getTabCount).map(tabs.getTabAt)
        .foreach(_.getCustomView.findViewById[TextView](android.R.id.text1)
          .setTextColor(getColorStateList(R.color.tab_text_color_dark)))
      findById[LinearLayout](rootView, R.id.top_background_layout).setBackgroundColor(getColor(R.color.white))
    }

    searchUserController.setFilter("")
    integrationsController.searchQuery ! ""

    (for {
      integrationTab <- searchResultAdapter.peopleOrServices
      hasSearch <- integrationsController.searchQuery.map(_.nonEmpty)
      hasResults <- integrationsController.searchIntegrations.map(_.forall(_.nonEmpty))
    } yield integrationTab && hasSearch && !hasResults).onUi {
      case true =>
        showErrorMessage()
      case false =>
        hideErrorMessage()
    }
    emptyListButton
  }

  override def onStart(): Unit = {
    super.onStart()
    getControllerFactory.getGlobalLayoutController.addKeyboardVisibilityObserver(this)
    if (isAddingToConversation && !isGroupConversation) {
      new Handler().post(new Runnable() {
        def run(): Unit = {
          searchUserController.selectedUsers.map(UserSignal(_)).foreach{ userSignal =>
            userSignal.head.map(data => searchBoxView.addElement(PickableUser(data)))(Threading.Ui)
          }
        }
      })
    }
    if (!isAddingToConversation && isPrivateAccount){
      implicit val ec = Threading.Ui
      zms.head.flatMap(_.userPrefs.preference(UserPreferences.ShareContacts).apply()).map{ showShareContactsDialog }
    }
  }

  override def onResume(): Unit = {
    super.onResume()
    inviteButton.setOnClickListener(this)
    userSelectionConfirmationButton.setOnClickListener(this)
    if (!isAddingToConversation) {
      new Handler().postDelayed(new Runnable() {
        def run(): Unit =
          Option(getStoreFactory).filterNot(_.isTornDown).foreach { store =>
            val numberOfActiveConversations: Int = store.conversationStore.numberOfActiveConversations
            Option(searchBoxView).filter(_ => numberOfActiveConversations > PickUserFragment.SHOW_KEYBOARD_THRESHOLD).foreach { v =>
              if (isTeamAccount) {
                v.setFocus()
                KeyboardUtils.showKeyboard(getActivity)
              }
            }
        }
      }, getInt(R.integer.people_picker__keyboard__show_delay))
    }
  }

  override def onPause(): Unit = {
    inviteButton.setOnClickListener(null)
    userSelectionConfirmationButton.setOnClickListener(null)
    super.onPause()
  }

  override def onStop(): Unit = {
    getContainer.getLoadingViewIndicator.hide()
    getControllerFactory.getGlobalLayoutController.removeKeyboardVisibilityObserver(this)
    super.onStop()
  }

  override def onBackPressed: Boolean =
    if (isKeyboardVisible) {
      KeyboardUtils.hideKeyboard(getActivity)
      true
    } else if (pickUserController.isShowingUserProfile) {
      pickUserController.hideUserProfile()
      true
    } else false

  override def onKeyboardVisibilityChanged(keyboardIsVisible: Boolean, keyboardHeight: Int, currentFocus: View): Unit = {
    isKeyboardVisible = keyboardIsVisible
    val color =
      if (keyboardIsVisible || !searchBoxIsEmpty)
        ContextCompat.getColor(getContext, R.color.people_picker__loading__color)
      else
        accentColor.currentValue.getOrElse(0)

    if (!isAddingToConversation) {
      getContainer.getLoadingViewIndicator.setColor(color)
      val inviteVisibility =
        if (keyboardIsVisible || searchUserController.selectedUsers.nonEmpty || isTeamAccount) View.GONE
        else View.VISIBLE
      inviteButton.setVisibility(inviteVisibility)
    }
  }

  private def createAndOpenConversation(users: Seq[UserId], requester: ConversationChangeRequester): Unit =
    userAccountsController.createAndOpenConversation(users.toArray, requester, getActivity.asInstanceOf[BaseActivity])

  override def onConversationButtonClicked(): Unit = {
    KeyboardUtils.hideKeyboard(getActivity)
    val users = if (isAddingToConversation) getSelectedAndExcluded else getSelectedUsers
    createAndOpenConversation(users.toSeq, ConversationChangeRequester.START_CONVERSATION)
  }

  override def onVideoCallButtonClicked(): Unit = {
    KeyboardUtils.hideKeyboard(getActivity)
    val users = if (isAddingToConversation) getSelectedAndExcluded else getSelectedUsers
    if (users.size > 1) {
      throw new IllegalStateException("A video call cannot be started with more than one user. The button should not be visible " + "if multiple users are selected.")
    }
    createAndOpenConversation(users.toSeq, ConversationChangeRequester.START_CONVERSATION_FOR_VIDEO_CALL)
  }

  override def onCallButtonClicked(): Unit = {
    KeyboardUtils.hideKeyboard(getActivity)
    val users = if (isAddingToConversation) getSelectedAndExcluded else getSelectedUsers
    createAndOpenConversation(users.toSeq, ConversationChangeRequester.START_CONVERSATION_FOR_CALL)
  }

  override def onCameraButtonClicked(): Unit = {
    KeyboardUtils.hideKeyboard(getActivity)
    val users = if (isAddingToConversation) getSelectedAndExcluded else getSelectedUsers
    createAndOpenConversation(users.toSeq, ConversationChangeRequester.START_CONVERSATION_FOR_CAMERA)
  }

  def onSearchBoxIsEmpty(): Unit = {
    searchBoxIsEmpty = true
    lastInputIsKeyboardDoneAction = false
    setConversationQuickMenuVisible(false)
  }

  def onSearchBoxHasNewSearchFilter(filter: String): Unit = {
    searchBoxIsEmpty = filter.isEmpty
    lastInputIsKeyboardDoneAction = false
  }

  def onKeyboardDoneAction(): Unit = {
    lastInputIsKeyboardDoneAction = true
    val users = searchUserController.selectedUsers
    val minUsers =
    if (isAddingToConversation && !isGroupConversation) 2 else 1
    if (users.size >= minUsers) {
      KeyboardUtils.hideKeyboard(getActivity)
      getContainer.onSelectedUsers(users.toSeq.asJava, ConversationChangeRequester.START_CONVERSATION)
    }
  }

  def onSelectedUserAdded(addedUser: UserId): Unit = {
    changeUserSelectedState(addedUser, selected = true)
    updateConversationButtonLabel()
    conversationQuickMenu.showVideoCallButton(shouldVideoCallButtonBeDisplayed)
    getUser(addedUser).collect {
      case Some(userData) => searchBoxView.addElement(PickableUser(userData))
    } (Threading.Ui)
  }

  private def shouldVideoCallButtonBeDisplayed: Boolean = {
    getSelectedUsers.size == 1 && DeviceDetector.isVideoCallingEnabled
  }

  def onSelectedUserRemoved(removedUser: UserId): Unit = {
    changeUserSelectedState(removedUser, selected = false)
    updateConversationButtonLabel()
    conversationQuickMenu.showVideoCallButton(shouldVideoCallButtonBeDisplayed)
    getUser(removedUser).collect {
      case Some(userData) => searchBoxView.removeElement(PickableUser(userData))
    } (Threading.Ui)
  }

  private def changeUserSelectedState(user: UserId, selected: Boolean): Unit = {
    changeUserSelectedState(searchResultRecyclerView, user, selected)
    if (!searchUserController.allDataSignal.currentValue.exists(_._1.isEmpty)) {
      (0 until searchResultRecyclerView.getChildCount).map(searchResultRecyclerView.getChildAt).foreach {
        case view: RecyclerView =>
          changeUserSelectedState(view, user, selected)
        case _ =>
      }
    }
  }

  private def changeUserSelectedState(rv: RecyclerView, user: UserId, selected: Boolean): Unit = {
    (0 until rv.getChildCount).map(rv.getChildAt).foreach{
      case userRowView: UserRowView if userRowView.getUser.contains(user) =>
        userRowView.setSelected(selected)
      case _ =>
    }
  }

  private def getUser(id: UserId): Future[Option[UserData]] = zms.head.flatMap(_.usersStorage.get(id))(Threading.Background)

  override def onUserClicked(userId: UserId, position: Int, anchorView: View): Unit =
      UserSignal(userId).head.map { userData =>
        if (userData.connection == ConnectionStatus.Accepted || (isTeamAccount && userAccountsController.isTeamMember(userData.id))) {
          if (anchorView.isSelected) searchUserController.addUser(userId) else searchUserController.removeUser(userId)
          setConversationQuickMenuVisible(searchUserController.selectedUsers.nonEmpty)
        } else if (!anchorView.isInstanceOf[ContactRowView] || (userData.connection != ConnectionStatus.Unconnected)) {
          showUser(userId, anchorView)
        }
      }(Threading.Ui)

  override def onUserDoubleClicked(userId: UserId, position: Int, anchorView: View): Future[Unit] =
    if (anchorView.isInstanceOf[ChatheadWithTextFooter] && searchUserController.selectedUsers.isEmpty) {
      import Threading.Implicits.Ui
      for {
        self <- self.head
        optUser <- getUser(userId)
        res  <- optUser match {
          case Some(user) if user.id != self.id && user.connection == UserData.ConnectionStatus.Accepted =>
            conversationController.getOrCreateConv(userId).flatMap { conv =>
              verbose(s"onConversationClicked(${conv.id})")
              conversationController.selectConv(Some(conv.id), ConversationChangeRequester.START_CONVERSATION)
            }
          case _ => Future.successful({})
        }
      } yield res
  } else Future.successful({})

  override def onConversationClicked(conversationData: ConversationData, position: Int): Unit = {
    KeyboardUtils.hideKeyboard(getActivity)
    verbose(s"onConversationClicked(${conversationData.id})")
    conversationController.selectConv(Some(conversationData.id), ConversationChangeRequester.START_CONVERSATION)
  }

  override def getSelectedUsers: Set[UserId] = searchUserController.selectedUsers

  def getSelectedAndExcluded: Set[UserId] = searchUserController.selectedUsers ++ searchUserController.excludedUsers.currentValue.getOrElse(Set[UserId]())

  override def onContactListUserClicked(userId: UserId): Unit = {
    import Threading.Implicits.Ui
      getUser(userId).collect{ case Some(u) => u }.flatMap { user =>
        user.connection match {
        case ConnectionStatus.Unconnected =>
          self.head.map { self =>
            val myName: String = self.getDisplayName
            val message: String = getString(R.string.connect__message, user.getDisplayName, myName)
            zms(_.connection.connectToUser(user.id, message, self.getDisplayName))
          }
        case _ =>
            Future.successful({})
      }
    }
  }

  override def onContactListContactClicked(contactDetails: ContactDetails): Unit = {
    if (inject[NetworkModeService].isOnlineMode) {
      contactDetails.contactMethods.toList match {
        case method :: Nil if method.getType == ContactMethod.Phone =>
          sendSMSInvite(method)
        case method :: Nil if method.getType == ContactMethod.Email =>
          sendEmailInvite(method)
        case methods if methods.nonEmpty =>
          val itemNames: Array[CharSequence] = methods.map(_.stringRepresentation).toArray

          var dialog: AlertDialog = null
          val builder: AlertDialog.Builder = new AlertDialog.Builder(getActivity)
          builder.setTitle(getResources.getString(R.string.people_picker__contact_list__invite_dialog__title))
            .setPositiveButton(getResources.getText(R.string.confirmation_menu__confirm_done), new DialogInterface.OnClickListener() {
            def onClick(dialogInterface: DialogInterface, i: Int): Unit = {
              val lv: ListView = dialog.getListView
              val selected: Int = lv.getCheckedItemPosition

              if (contactDetails.contactMethods.isDefinedAt(selected)) {
                val selectedContactMethod = contactDetails.contactMethods(selected)
                if (selectedContactMethod.getType == ContactMethod.Phone) {
                  sendSMSInvite(selectedContactMethod)
                } else {
                  sendEmailInvite(selectedContactMethod)
                }
              }
            }
          }).setNegativeButton(getResources.getText(R.string.confirmation_menu__cancel), new DialogInterface.OnClickListener() {
            def onClick(dialogInterface: DialogInterface, i: Int): Unit = {
              dialogInterface.cancel()
            }
          }).setSingleChoiceItems(itemNames, PickUserFragment.DEFAULT_SELECTED_INVITE_METHOD, null)
          dialog = builder.create
          dialog.show()
        case _ =>
      }
    }
  }

  private def sendEmailInvite(contactMethod: ContactMethod) = {
    contactsController.invite(contactMethod, " ", null)
    Toast.makeText(getActivity, getResources.getString(R.string.people_picker__invite__sent_feedback), Toast.LENGTH_LONG).show()
  }

  private def sendSMSInvite(contactMethod: ContactMethod): Unit = {
    self.head.map { self =>
      val smsBody = self.handle.map(_.string) match {
        case Some(username) =>
          getString(R.string.people_picker__invite__share_text__body, StringUtils.formatHandle(username))
        case _ =>
          getString(R.string.people_picker__invite__share_text__body, self.getDisplayName)
      }
      val intent: Intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("sms", contactMethod.stringRepresentation, ""))
      intent.putExtra("sms_body", smsBody)
      startActivity(intent)
    } (Threading.Ui)
  }

  private def showErrorMessage(): Unit =
    errorMessageView.setVisibility(View.VISIBLE)

  private def hideErrorMessage(): Unit =
    errorMessageView.setVisibility(View.GONE)

  private def getCurrentPickerDestination: IPickUserController.Destination = {
    getContainer.getCurrentPickerDestination
  }

  private def sendGenericInvite(fromSearch: Boolean): Unit =
    Option(getControllerFactory).filterNot(_.isTornDown).foreach { _ =>
      self.head.map { self =>
        val username = self.handle.map(_.string).getOrElse("")
        val sharingIntent = IntentUtils.getInviteIntent(getString(R.string.people_picker__invite__share_text__header, self.getDisplayName), getString(R.string.people_picker__invite__share_text__body, StringUtils.formatHandle(username)))
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.people_picker__invite__share_details_dialog)))
      }(Threading.Ui)
    }

  private def showUser(user: UserId, anchorView: View): Unit = {
    import Threading.Implicits.Ui
    getUser(user).collect{ case Some(u) => u.connection }.map {
      case ConnectionStatus.Accepted =>
        if (isAddingToConversation) {
          getContainer.onSelectedUsers(Seq(user).asJava, ConversationChangeRequester.START_CONVERSATION)
        } else {
          val convId = ConvId(user.str) //TODO: This way of getting the id should be defined in the AccountService or something
          KeyboardUtils.hideKeyboard(getActivity)
          verbose(s"showUser $convId")
          conversationController.selectConv(convId, ConversationChangeRequester.START_CONVERSATION)
        }
      case ConnectionStatus.PendingFromUser |
           ConnectionStatus.Blocked |
           ConnectionStatus.Ignored |
           ConnectionStatus.Cancelled |
           ConnectionStatus.Unconnected =>
        KeyboardUtils.hideKeyboard(getActivity)
        convScreenController.setPopoverLaunchedMode(DialogLaunchMode.SEARCH)
        pickUserController.showUserProfile(user, anchorView)
      case ConnectionStatus.PendingFromOther =>
        val convId = ConvId(user.str) //TODO: This way of getting the id should be defined in the AccountService or something
        KeyboardUtils.hideKeyboard(getActivity)
        getContainer.showIncomingPendingConnectRequest(convId)
      case _ =>
    }
  }

  private def setConversationQuickMenuVisible(show: Boolean): Unit = {
    if (getView != null && getControllerFactory != null && !getControllerFactory.isTornDown) {
      if (isAddingToConversation) {
        userSelectionConfirmationButton.setVisibility(if (getSelectedUsers.nonEmpty) View.VISIBLE else View.GONE)
      } else {
        val visible: Boolean = show || searchUserController.selectedUsers.nonEmpty
        conversationQuickMenu.setVisibility(if (visible) View.VISIBLE else View.GONE)
        inviteButton.setVisibility(if (visible || isKeyboardVisible || isTeamAccount) View.GONE else View.VISIBLE)
      }
    }
  }

  private def updateConversationButtonLabel(): Unit = {
    val label: String = if (searchUserController.selectedUsers.size > 1) getString(R.string.conversation_quick_menu__conversation_button__group_label)
    else getString(R.string.conversation_quick_menu__conversation_button__single_label)
    conversationQuickMenu.setConversationButtonText(label)
    val hasPermissions = isPrivateAccount || searchUserController.selectedUsers.size == 1 || teamPermissions.contains(AccountData.Permission.CreateConversation)
    conversationQuickMenu.setVisibility(if (hasPermissions && !isAddingToConversation) View.VISIBLE else View.GONE)
  }

  override def onClick(view: View): Unit = {
    view.getId match {
      case R.id.confirmation_button =>
        KeyboardUtils.hideKeyboard(getActivity)
        val users = if (isGroupConversation) searchUserController.selectedUsers else getSelectedAndExcluded
        getContainer.onSelectedUsers(users.toSeq.asJava, ConversationChangeRequester.START_CONVERSATION)
      case R.id.invite_button =>
        sendGenericInvite(false)
    }
  }

  private def isGroupConversation: Boolean =
    getArguments.getBoolean(PickUserFragment.ARGUMENT_GROUP_CONVERSATION)

  private def isAddingToConversation: Boolean = {
    getArguments.getBoolean(PickUserFragment.ARGUMENT_ADD_TO_CONVERSATION)
  }

  private def addingToConversation: Option[ConvId] = {
    Option(getArguments.getString(PickUserFragment.ARGUMENT_CONVERSATION_ID)).map(ConvId(_))
  }

  private def isPrivateAccount: Boolean = !isTeamAccount

  private def isTeamAccount: Boolean = userAccountsController.isTeamAccount
  private def closeStartUI(): Unit = {
    KeyboardUtils.hideKeyboard(getActivity)
    searchUserController.setFilter("")
    integrationsController.searchQuery ! ""
    searchResultAdapter.peopleOrServices ! false
    pickUserController.hidePickUser(getCurrentPickerDestination)
  }


  // XXX Only show contact sharing dialogs for PERSONAL START UI
  private def showShareContactsDialog(hasShareContactsEnabled: Boolean): Unit = {
    val prefController = getControllerFactory.getUserPreferencesController
    // Doesn't have _our_ contact sharing setting enabled, maybe show dialog
    if (!hasShareContactsEnabled && !prefController.hasPerformedAction(IUserPreferencesController.DO_NOT_SHOW_SHARE_CONTACTS_DIALOG)) {
      // show initial dialog
      val checkBoxView= View.inflate(getContext, R.layout.dialog_checkbox, null)
      val checkBox = checkBoxView.findViewById(R.id.checkbox).asInstanceOf[CheckBox]
      val checkedItems = new java.util.HashSet[Integer]
      checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        def onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean): Unit =  {
          if (isChecked)
            checkedItems.add(1)
          else
            checkedItems.remove(1)
        }
      })
      checkBox.setText(R.string.people_picker__share_contacts__nevvah)
      val dialog = new AlertDialog.Builder(getContext).setTitle(R.string.people_picker__share_contacts__title).setMessage(R.string.people_picker__share_contacts__message).setView(checkBoxView).setPositiveButton(R.string.people_picker__share_contacts__yay, new DialogInterface.OnClickListener() {
        def onClick(dialog: DialogInterface, which: Int): Unit = {
          inject[PermissionsService].requestAllPermissions(Set(READ_CONTACTS)).map {
            case true =>
              updateShareContacts(true)

            case _ =>
              updateShareContacts(false)
              val showRationale = shouldShowRequestPermissionRationale(READ_CONTACTS)
              if (!showRationale && getControllerFactory != null && !getControllerFactory.isTornDown) {
                getControllerFactory.getUserPreferencesController.setPerformedAction(
                  IUserPreferencesController.DO_NOT_SHOW_SHARE_CONTACTS_DIALOG)
              }
          } (Threading.Ui)
        }
      }).setNegativeButton(R.string.people_picker__share_contacts__nah, new DialogInterface.OnClickListener() {
        def onClick(dialog: DialogInterface, which: Int): Unit = {
          if (getControllerFactory != null && !getControllerFactory.isTornDown && checkedItems.size > 0) getControllerFactory.getUserPreferencesController.setPerformedAction(IUserPreferencesController.DO_NOT_SHOW_SHARE_CONTACTS_DIALOG)
        }
      }).create
      dialog.show()
    }
  }

  private def updateShareContacts(share: Boolean): Unit ={
    zms.head.flatMap(_.userPrefs.preference(UserPreferences.ShareContacts).update(share)) (Threading.Background)
  }

  override def onIntegrationClicked(data: IntegrationData): Unit = {
    KeyboardUtils.hideKeyboard(getActivity)
    verbose(s"onIntegrationClicked(${data.id})")

    val detailsController = inject[IntegrationDetailsController]
    addingToConversation.fold(detailsController.setPicking())(detailsController.setAdding)
    if (isAddingToConversation) {
      convScreenController.showIntegrationDetails(data.provider, data.id)
    } else {
      import IntegrationDetailsFragment._
      getFragmentManager.beginTransaction
        .setCustomAnimations(
          R.anim.slide_in_from_bottom_pick_user,
          R.anim.open_new_conversation__thread_list_out,
          R.anim.open_new_conversation__thread_list_in,
          R.anim.slide_out_to_bottom_pick_user)
        .replace(R.id.fl__conversation_list_main, newInstance(data.provider, data.id), Tag)
        .addToBackStack(Tag)
        .commit()

      getControllerFactory.getNavigationController.setLeftPage(Page.INTEGRATION_DETAILS, PickUserFragment.TAG)
    }
  }
}
