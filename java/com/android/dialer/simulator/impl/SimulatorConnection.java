/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.dialer.simulator.impl;

import android.content.Context;
import android.support.annotation.NonNull;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.VideoProfile;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.dialer.simulator.Simulator.Event;
import java.util.ArrayList;
import java.util.List;

/** Represents a single phone call on the device. */
public final class SimulatorConnection extends Connection {
  private final List<Listener> listeners = new ArrayList<>();
  private final List<Event> events = new ArrayList<>();
  private int currentState = STATE_NEW;

  SimulatorConnection(@NonNull Context context, @NonNull ConnectionRequest request) {
    Assert.isNotNull(context);
    Assert.isNotNull(request);
    putExtras(request.getExtras());
    setConnectionCapabilities(
        CAPABILITY_MUTE
            | CAPABILITY_SUPPORT_HOLD
            | CAPABILITY_HOLD
            | CAPABILITY_CAN_UPGRADE_TO_VIDEO);
    setVideoProvider(new SimulatorVideoProvider(context, this));
  }

  public void addListener(@NonNull Listener listener) {
    listeners.add(Assert.isNotNull(listener));
  }

  public void removeListener(@NonNull Listener listener) {
    listeners.remove(Assert.isNotNull(listener));
  }

  @NonNull
  public List<Event> getEvents() {
    return events;
  }

  @Override
  public void onAnswer(int videoState) {
    LogUtil.enterBlock("SimulatorConnection.onAnswer");
    onEvent(new Event(Event.ANSWER, Integer.toString(videoState), null));
  }

  @Override
  public void onReject() {
    LogUtil.enterBlock("SimulatorConnection.onReject");
    onEvent(new Event(Event.REJECT));
  }

  @Override
  public void onHold() {
    LogUtil.enterBlock("SimulatorConnection.onHold");
    onEvent(new Event(Event.HOLD));
  }

  @Override
  public void onUnhold() {
    LogUtil.enterBlock("SimulatorConnection.onUnhold");
    onEvent(new Event(Event.UNHOLD));
  }

  @Override
  public void onDisconnect() {
    LogUtil.enterBlock("SimulatorConnection.onDisconnect");
    onEvent(new Event(Event.DISCONNECT));
  }

  @Override
  public void onStateChanged(int newState) {
    LogUtil.i(
        "SimulatorConnection.onStateChanged",
        "%s -> %s",
        stateToString(currentState),
        stateToString(newState));
    int oldState = currentState;
    currentState = newState;
    onEvent(new Event(Event.STATE_CHANGE, stateToString(oldState), stateToString(newState)));
  }

  @Override
  public void onPlayDtmfTone(char c) {
    LogUtil.enterBlock("SimulatorConnection.onPlayDtmfTone");
    onEvent(new Event(Event.DTMF, Character.toString(c), null));
  }

  void onEvent(@NonNull Event event) {
    events.add(Assert.isNotNull(event));
    for (Listener listener : listeners) {
      listener.onEvent(this, event);
    }
  }

  void handleSessionModifyRequest(@NonNull Event event) {
    VideoProfile fromProfile = new VideoProfile(Integer.parseInt(event.data1));
    VideoProfile toProfile = new VideoProfile(Integer.parseInt(event.data2));
    setVideoState(toProfile.getVideoState());
    getVideoProvider()
        .receiveSessionModifyResponse(
            Connection.VideoProvider.SESSION_MODIFY_REQUEST_SUCCESS, fromProfile, toProfile);
  }

  /** Callback for when a new event arrives. */
  public interface Listener {
    void onEvent(@NonNull SimulatorConnection connection, @NonNull Event event);
  }
}