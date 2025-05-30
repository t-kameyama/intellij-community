// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.gitlab.ui.clone.model

import com.intellij.collaboration.async.mapState
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.platform.util.coroutines.childScope
import git4idea.checkout.GitCloneUtils
import git4idea.ui.GitShallowCloneViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.jetbrains.plugins.gitlab.api.GitLabApiManager
import org.jetbrains.plugins.gitlab.authentication.accounts.GitLabAccountManager
import org.jetbrains.plugins.gitlab.authentication.ui.GitLabAccountsDetailsProvider
import org.jetbrains.plugins.gitlab.ui.clone.GitLabCloneListItem
import org.jetbrains.plugins.gitlab.ui.clone.model.GitLabCloneRepositoriesViewModel.SearchModel
import java.net.MalformedURLException
import java.net.URL

internal interface GitLabCloneRepositoriesViewModel : GitLabClonePanelViewModel {
  val listVm: GitLabCloneRepositoriesListViewModel

  val searchValue: SharedFlow<SearchModel>
  val selectedUrl: SharedFlow<String?>

  val accountDetailsProvider: GitLabAccountsDetailsProvider

  val shallowCloneVm: GitShallowCloneViewModel

  fun selectItem(item: GitLabCloneListItem?)

  fun setSearchValue(text: String)

  fun setDirectoryPath(path: String)

  fun doClone(checkoutListener: CheckoutProvider.Listener)

  sealed interface SearchModel {
    class Url(val url: String) : SearchModel
    object Text : SearchModel
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class GitLabCloneRepositoriesViewModelImpl(
  private val project: Project,
  parentCs: CoroutineScope,
  private val accountManager: GitLabAccountManager,
) : GitLabCloneRepositoriesViewModel {
  private val apiManager: GitLabApiManager = service<GitLabApiManager>()

  private val cs: CoroutineScope = parentCs.childScope(javaClass.name)

  override val listVm: GitLabCloneRepositoriesListViewModel = GitLabCloneRepositoriesListViewModelImpl(cs, accountManager)

  private val selectedItem: MutableStateFlow<GitLabCloneListItem?> = MutableStateFlow(null)

  private val _searchValue: MutableStateFlow<String> = MutableStateFlow("")
  override val searchValue: SharedFlow<SearchModel> = _searchValue.mapState(cs) { text ->
    // TODO: implement ssh "git@"
    try {
      URL(text) // Check URL correctness
      SearchModel.Url(text)
    }
    catch (_: MalformedURLException) {
      SearchModel.Text
    }
  }

  private val _selectedUrl: StateFlow<String?> = combine(searchValue, selectedItem) { searchValue, selectedItem ->
    when {
      searchValue is SearchModel.Url -> searchValue.url
      selectedItem != null && selectedItem is GitLabCloneListItem.Repository -> selectedItem.project.httpUrlToRepo
      else -> null
    }
  }.stateIn(cs, SharingStarted.Eagerly, initialValue = null)
  override val selectedUrl: SharedFlow<String?> = _selectedUrl

  private val directoryPath: MutableStateFlow<String> = MutableStateFlow("")

  override val shallowCloneVm = GitShallowCloneViewModel()

  override val accountDetailsProvider = GitLabAccountsDetailsProvider(cs, accountManager) { account ->
    val token = accountManager.findCredentials(account) ?: return@GitLabAccountsDetailsProvider null
    apiManager.getClient(account.server) { token }
  }

  override fun selectItem(item: GitLabCloneListItem?) {
    selectedItem.value = item
  }

  override fun setSearchValue(text: String) {
    _searchValue.value = text
  }

  override fun setDirectoryPath(path: String) {
    directoryPath.value = path
  }

  override fun doClone(checkoutListener: CheckoutProvider.Listener) {
    val selectedUrl = _selectedUrl.value ?: error("Clone button is enabled when repository is not selected")
    GitCloneUtils.clone(project, selectedUrl, directoryPath.value, shallowCloneVm.getShallowCloneOptions(), checkoutListener,
                        CLONE_UNABLE_TO_CREATE_DESTINATION_DIRECTORY,
                        CLONE_UNABLE_TO_FIND_DESTINATION_DIRECTORY)
  }

  companion object {
    private const val CLONE_UNABLE_TO_CREATE_DESTINATION_DIRECTORY = "gitlab.clone.unable.to.create.destination.directory"
    private const val CLONE_UNABLE_TO_FIND_DESTINATION_DIRECTORY = "gitlab.clone.unable.to.find.destination.directory"
  }
}