package com.zxm.core.base

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.zxm.core.R

/**
 * Created by ZhangXinmin on 2020/7/19.
 * Copyright (c) 2020 . All rights reserved.
 */
abstract class BaseFragment() : Fragment(), FragmentLazyLifecycleOwner.Callback {

    protected val sTAG = this.javaClass.simpleName

    protected var mContext: Context? = null

    //lifecycle
    private var mLazyViewLifecycleOwner: FragmentLazyLifecycleOwner? = null

    //animation
    private var mCalled = true
    private var mEnterAnimationStatus: Int =
        ANIMATION_ENTER_STATUS_NOT_START
    private val isInEnterAnimationLiveData = MutableLiveData<Boolean>()

    //back press
    private var mOnBackPressedDispatcher: OnBackPressedDispatcher? = null
    private val mOnBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {

            override fun handleOnBackPressed() {
                onNormalBackPressed()
            }

        }


    abstract fun setLayoutId(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context

        mOnBackPressedDispatcher = requireActivity().onBackPressedDispatcher
        mOnBackPressedDispatcher?.addCallback(this, mOnBackPressedCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return setLayoutId(inflater, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mLazyViewLifecycleOwner = FragmentLazyLifecycleOwner(this)
        mLazyViewLifecycleOwner?.let {
            viewLifecycleOwner.lifecycle.addObserver(it)
        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        isInEnterAnimationLiveData.postValue(false)

        initParamsAndValues()

    }

    protected open fun initParamsAndValues() {}


    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        if (!enter) {
            // This is a workaround for the bug where child value disappear when
            // the parent is removed (as all children are first removed from the parent)
            // See https://code.google.com/p/android/issues/detail?id=55228
            var rootParentFragment: Fragment? = null
            var parentFragment = parentFragment
            while (parentFragment != null) {
                rootParentFragment = parentFragment
                parentFragment = parentFragment.parentFragment
            }
            if (rootParentFragment != null && rootParentFragment.isRemoving) {
                val doNothingAnim: Animation = AlphaAnimation(1f, 1f)
                val duration = resources.getInteger(R.integer.qmui_anim_duration)
                doNothingAnim.duration = duration.toLong()
                return doNothingAnim
            }
        }
        var animation: Animation? = null
        if (enter) {
            try {
                animation = AnimationUtils.loadAnimation(context, nextAnim)
            } catch (ignored: Throwable) {
            }
            if (animation != null) {
                animation.setAnimationListener(object : AnimationListener {
                    override fun onAnimationStart(animation: Animation) {
                        checkAndCallOnEnterAnimationStart(animation)
                    }

                    override fun onAnimationEnd(animation: Animation) {
                        checkAndCallOnEnterAnimationEnd(animation)
                    }

                    override fun onAnimationRepeat(animation: Animation) {}
                })
            } else {
                checkAndCallOnEnterAnimationStart(null)
                checkAndCallOnEnterAnimationEnd(null)
            }
        }
        return animation
    }


    fun isAttachedToActivity(): Boolean {
        return !isRemoving
    }

    companion object {

        val SLIDE_TRANSITION_CONFIG: TransitionConfig =
            TransitionConfig(
                R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
            )

        val SCALE_TRANSITION_CONFIG: TransitionConfig =
            TransitionConfig(
                R.anim.scale_enter, R.anim.slide_still,
                R.anim.slide_still, R.anim.scale_exit
            )

        const val ANIMATION_ENTER_STATUS_NOT_START = -1
        const val ANIMATION_ENTER_STATUS_STARTED = 0
        const val ANIMATION_ENTER_STATUS_END = 1
    }

    //===============================start fragment=======================================//

    /**
     * start a new fragment and add to BackStack
     *
     * @param fragment the fragment to start
     * @return Returns the identifier of this transaction's back stack entry,
     * if [FragmentTransaction.addToBackStack] had been called.  Otherwise, returns
     * a negative number.
     */
    fun startFragment(fragment: BaseFragment): Int {
        if (!checkStateLoss("startFragment")) {
            return -1
        }
        val provider = findFragmentContainerProvider()
//            ?: return if (BuildConfig.DEBUG) {
            ?: return if (true) {
                throw java.lang.RuntimeException("Can not find the fragment container provider.")
            } else {
                Log.d(sTAG, "Can not find the fragment container provider.")
                -1
            }
        return startFragment(fragment, provider)
    }

    private fun startFragment(
        fragment: BaseFragment,
        provider: FragmentContainerProvider
    ): Int {
        val transitionConfig: TransitionConfig = fragment.onFetchTransitionConfig()
        val tagName: String = fragment.javaClass.simpleName
        return provider.getContainerFragmentManager()
            .beginTransaction()
            .setPrimaryNavigationFragment(null)
            .setCustomAnimations(
                transitionConfig.enter,
                transitionConfig.exit,
                transitionConfig.popEnter,
                transitionConfig.popExit
            )
            .replace(provider.getContainerViewId(), fragment, tagName)
            .addToBackStack(tagName)
            .commit()
    }

    private fun checkStateLoss(logName: String): Boolean {
        if (!isAdded) {
            return false
        }

        if (parentFragmentManager.isStateSaved) {
            Log.e(sTAG, "$logName can not be invoked after onSaveInstanceState")
            return false
        }
        return true

    }

    //===============================lazy==================================================//
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        notifyFragmentVisibleToUserChanged(isVisibleToUser && isParentVisibleToUser())
    }

    override fun isVisibleToUser(): Boolean {
        return userVisibleHint && isParentVisibleToUser()
    }

    /**
     * @return true if parentFragments is visible to user
     */
    private fun isParentVisibleToUser(): Boolean {
        var parentFragment = parentFragment
        while (parentFragment != null) {
            if (!parentFragment.userVisibleHint) {
                return false
            }
            parentFragment = parentFragment.parentFragment
        }
        return true
    }

    private fun notifyFragmentVisibleToUserChanged(isVisibleToUser: Boolean) {
        if (mLazyViewLifecycleOwner != null) {
            mLazyViewLifecycleOwner!!.setViewVisible(isVisibleToUser)
        }
        if (isAdded) {
            val childFragments = childFragmentManager.fragments
            for (fragment in childFragments) {
                if (fragment is BaseFragment) {
                    (fragment as BaseFragment).notifyFragmentVisibleToUserChanged(
                        isVisibleToUser && fragment.userVisibleHint
                    )
                }
            }
        }
    }

    //=====================================back press==========================================//
    protected fun onNormalBackPressed() {
        if (parentFragment != null) {
            bubbleBackPressedEvent()
            return
        }
        val activity: Activity = requireActivity()
        if (activity is FragmentContainerProvider) {
            val provider: FragmentContainerProvider = activity
            if (provider.getContainerFragmentManager()
                    .backStackEntryCount > 1 || provider.getContainerFragmentManager()
                    .primaryNavigationFragment === this
            ) {
                bubbleBackPressedEvent()
            } else {
                val transitionConfig: TransitionConfig = onFetchTransitionConfig()

                requireActivity().finish()
                requireActivity().overridePendingTransition(
                    transitionConfig.popEnter,
                    transitionConfig.popExit
                )
            }
        } else {
            bubbleBackPressedEvent()
        }
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }

    private fun bubbleBackPressedEvent() {
        // disable this and go with FragmentManager's backPressesCallback
        // because it will call execPendingActions before popBackStackImmediate
        mOnBackPressedCallback.isEnabled = false
        mOnBackPressedDispatcher?.onBackPressed()
        mOnBackPressedCallback.isEnabled = true
    }

    protected open fun findFragmentContainerProvider(): FragmentContainerProvider? {
        var parent = parentFragment
        while (parent != null) {
            parent = if (parent is FragmentContainerProvider) {
                return parent
            } else {
                parent.parentFragment
            }
        }
        val activity: Activity? = activity
        return if (activity is FragmentContainerProvider) {
            activity
        } else null
    }

    protected fun popBackStack() {
        mOnBackPressedDispatcher?.onBackPressed()
    }

    /**
     * pop back to a clazz type fragment
     *
     *
     * Assuming there is a back stack: Home -> List -> Detail. Perform popBackStack(Home.class),
     * Home is the current fragment
     *
     *
     * if the clazz type fragment doest not exist in back stack, this method is Equivalent
     * to popBackStack()
     *
     * @param cls the type of target fragment
     */
    protected open fun popBackStack(cls: Class<out BaseFragment?>) {
        if (checkPopBack()) {
            parentFragmentManager.popBackStack(cls.simpleName, 0)
        }
    }

    protected fun jumpActivity(clazz: Class<*>) {
        val intent = Intent(mContext, clazz)
        startActivity(intent)
    }

    private fun checkPopBack(): Boolean {
        return if (!isResumed || mEnterAnimationStatus != ANIMATION_ENTER_STATUS_END) {
            false
        } else checkStateLoss("popBackStack")
    }
    //======================================Animation===========================================//

    private fun checkAndCallOnEnterAnimationStart(animation: Animation?) {
        mCalled = false
        onEnterAnimationStart(animation)
        if (!mCalled) {
            throw RuntimeException(javaClass.simpleName + " did not call through to super.onEnterAnimationStart(Animation)")
        }
    }

    private fun checkAndCallOnEnterAnimationEnd(animation: Animation?) {
        mCalled = false
        onEnterAnimationEnd(animation)
        if (!mCalled) {
            throw RuntimeException(javaClass.simpleName + " did not call through to super.onEnterAnimationEnd(Animation)")
        }
    }

    protected fun onEnterAnimationStart(animation: Animation?) {
        if (mCalled) {
            throw IllegalAccessError("don't call #onEnterAnimationStart() directly")
        }
        mCalled = true
        mEnterAnimationStatus = ANIMATION_ENTER_STATUS_STARTED
        isInEnterAnimationLiveData.setValue(true)
    }

    protected fun onEnterAnimationEnd(animation: Animation?) {
        if (mCalled) {
            throw IllegalAccessError("don't call #onEnterAnimationEnd() directly")
        }
        mCalled = true
        mEnterAnimationStatus = ANIMATION_ENTER_STATUS_END
        isInEnterAnimationLiveData.setValue(false)
    }

    protected fun <T> enterAnimationAvoidTransform(origin: LiveData<T>?): LiveData<T>? {
        return enterAnimationAvoidTransform(origin, isInEnterAnimationLiveData)
    }

    protected fun <T> enterAnimationAvoidTransform(
        origin: LiveData<T>?,
        enterAnimationLiveData: LiveData<Boolean>?
    ): LiveData<T>? {
        val result = MediatorLiveData<T>()
        result.addSource(enterAnimationLiveData!!, object : Observer<Boolean> {
            var isAdded = false
            override fun onChanged(isInEnterAnimation: Boolean) {
                if (isInEnterAnimation) {
                    isAdded = false
                    result.removeSource(origin!!)
                } else {
                    if (!isAdded) {
                        isAdded = true
                        result.addSource(
                            origin!!
                        ) { t -> result.value = t }
                    }
                }
            }
        })
        return result
    }

    fun onFetchTransitionConfig(): TransitionConfig {
        return SLIDE_TRANSITION_CONFIG
    }
}

/**
 * Specific animation resources to run for the fragments that are
 * entering and exiting in this transaction.The <code>popEnter</code>
 * and <code>popExit</code> animations will be played for enter/exit
 * operations specifically when popping the back stack.
 *
 * <pre class="prettyprint">
 *  fragmentManager.beingTransaction()
 *      .setCustomAnimations(enter1, exit1, popEnter1, popExit1)
 *      .add(MyFragmentClass, args, tag1) // this fragment gets the first animations
 *      .setCustomAnimations(enter2, exit2, popEnter2, popExit2)
 *      .add(MyFragmentClass, args, tag2) // this fragment gets the second animations
 *      .commit()
 * </pre>
 */
class TransitionConfig(val enter: Int, val exit: Int, val popEnter: Int, val popExit: Int) {
    constructor(enter: Int, popExit: Int) : this(enter, 0, 0, popExit)
}