package com.mathieupe;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extension.controller.ControllerExtension;

public class VCM600Extension extends ControllerExtension
{
   private TrackBank mMainTrackBank = null;
   private MidiIn mMidiIn = null;
   private MidiOut mMidiOut = null;

   private static final int MSG_CC = 11;
   private static final int MSG_NOTE_ON = 9;


   private Transport mTransport;
   private MasterTrack mMasterTrack;
   private TrackBank mEffectTrackBank;
   private final CursorDevice[] mMainDevices = new CursorDevice[6];
   private final CursorRemoteControlsPage[] mMainRemoteControlsPages = new CursorRemoteControlsPage[6];
   private Application mApplication;
   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mCursorRemoteControlsPage;

   protected VCM600Extension(final VCM600ExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mTransport = host.createTransport();
      mApplication = host.createApplication();
      mMasterTrack = host.createMasterTrack(0);
      mCursorTrack = host.createCursorTrack(0, 0);
      mCursorDevice = mCursorTrack.createCursorDevice();
      mCursorRemoteControlsPage = mCursorDevice.createCursorRemoteControlsPage(8);
      for (int i = 0; i < 8; ++i)
      {
         RemoteControl parameter = mCursorRemoteControlsPage.getParameter(i);
         parameter.markInterested();
         parameter.setIndication(true);
      }

      mMidiIn = getHost().getMidiInPort(0);
      mMidiOut = getHost().getMidiOutPort(0);

      mMidiIn.setMidiCallback(this::onMidiIn);

      mMainTrackBank = getHost().createMainTrackBank(6, 3, 1);

      for (int i = 0; i < 6; ++i)
      {
         Track channel = mMainTrackBank.getChannel(i);

         channel.clipLauncherSlotBank().setIndication(true);

         channel.getVolume().markInterested();
         channel.getPan().markInterested();
         channel.getMute().markInterested();
         channel.getSolo().markInterested();
         channel.isStopped().markInterested();
         channel.isQueuedForStop().markInterested();


         mMainDevices[i] = channel.createCursorDevice(); //"Channel Strip");
         mMainRemoteControlsPages[i] = mMainDevices[i].createCursorRemoteControlsPage(8);


         for (int j = 0; j < 8; ++j)
         {
            mMainRemoteControlsPages[i].getParameter(j).markInterested();
         }
      }

      mEffectTrackBank = host.createEffectTrackBank(3, 1);
      for (int i = 0; i < 3; ++i)
      {
         Track track = mEffectTrackBank.getItemAt(i);
         track.getPan().markInterested();
         track.getVolume().markInterested();
         track.getMute().markInterested();
      }

      // For now just show a popup notification for verification that it is running.
      getHost().showPopupNotification("VCM-600 Initialized");
   }

   @Override
   public void exit()
   {
      // TODO: Perform any cleanup once the driver exits
      // For now just show a popup notification for verification that it is no longer running.
      getHost().showPopupNotification("VCM-600 Exited");
   }

   @Override
   public void flush()
   {
      paintButtons();
   }

   void paintButtons()
   {
      // TODO Send any updates you need here.
      for (int i = 0; i < 6; ++i)
      {
         for (int j = 0; j < 3; ++j)
         {
            RemoteControl parameter = mMainRemoteControlsPages[i].getParameter(4 + j);
            mMidiOut.sendMidi((MSG_NOTE_ON << 4) + i, 60 + j, parameter.get() > 0 ? 0 : 127);
         }

         Track channel = mMainTrackBank.getChannel(i);

         SettableBooleanValue mute = channel.getMute();
         mMidiOut.sendMidi((MSG_NOTE_ON << 4) + i, 63, mute.get() ? 127 : 0);
         mMidiOut.sendMidi((MSG_NOTE_ON << 4) + i, 64, channel.getSolo().get() ? 127 : 0);
         mMidiOut.sendMidi((MSG_NOTE_ON << 4) + i, 68, channel.isQueuedForStop().get() ? 127 : 0);
         mMidiOut.sendMidi((MSG_NOTE_ON << 4) + i, 69, channel.isStopped().get() ? 0 : 127);
      }
   }


   private void onMidiIn(int status, int data1, int data2) {
      int channel = status & 0xF;
      int msg = status >> 4;

      getHost().println("MIDI IN msg: " + msg  + ", channel: " + channel + ", data1: " + data1 + ", data2: " + data2);

      switch (msg)
      {
         case MSG_CC: /* CC */
            if (channel == 12 && data1 == 7)
               mEffectTrackBank.getChannel(2).getVolume().set(data2, 128);
            else if (data1 == 8)
               mTransport.getCrossfade().set(data2 > 126 ? 126 : data2, 127);
            else if (0 <= channel && channel < 6 && data1 == 23)
               mMainTrackBank.getChannel(channel).getVolume().set(data2, 128);
            else if (0 <= channel && channel < 6 && 16 <= data1 && data1 <= 18)
               // Hi/Mid/Low per track
               mMainRemoteControlsPages[channel].getParameter(data1 - 16).set(data2, 128);
            else if (0 <= channel && channel < 6 && data1 == 10)
               mMainTrackBank.getChannel(channel).sendBank().getItemAt(0).set(data2, 128);
            else if (0 <= channel && channel < 6 && 19 <= data1 && data1 <= 20)
               mMainTrackBank.getChannel(channel).sendBank().getItemAt(1 + data1 - 19).set(data2, 128);
            else if (0 <= channel && channel < 6 && 21 == data1)
               mMainRemoteControlsPages[channel].getParameter(3).set(data2, 128);
            else if (0 <= channel && channel < 6 && 22 == data1)
               mMainRemoteControlsPages[channel].getParameter(7).set(data2, 128);
            else if (channel == 12 && 12 <= data1 && data1 <= 19)
               mCursorRemoteControlsPage.getParameter(data1 - 12).set(data2, 128);
            else if (channel == 12 && 20 <= data1 && data1 <= 21)
               mEffectTrackBank.getChannel(data1 - 20).getPan().set(data2, 128);
            else if (channel == 12 && data1 == 24)
               mEffectTrackBank.getChannel(2).getPan().set(data2, 128);
            else if (channel == 12 && 22 <= data1 && data1 <= 23)
               mEffectTrackBank.getChannel(data1 - 22).getVolume().set(data2, 128);
            if (channel == 12 && data1 == 26)
               mMasterTrack.getVolume().set(data2 > 126 ? 126 : data2, 127);
            break;

         case MSG_NOTE_ON:
            if (0 <= channel && channel < 6 && data1 == 63 && data2 == 127)
               mMainTrackBank.getChannel(channel).getMute().toggle();
            else if (0 <= channel && channel < 6 && data1 == 64 && data2 == 127)
               mMainTrackBank.getChannel(channel).getSolo().toggle();
            else if (0 <= channel && channel < 6 && data1 == 66 && data2 == 127) {
               // TODO: select current clip
               mMainTrackBank.getChannel(channel).clipLauncherSlotBank().getItemAt(0);
               mMainTrackBank.getChannel(channel).selectInMixer();
               mApplication.nextSubPanel();
            }
            else if (0 <= channel && channel < 6 && 60 <= data1 && data1 <= 62 && data2 == 127)
            {
               RemoteControl parameter = mMainRemoteControlsPages[channel].getParameter(4 + data1 - 60);
               parameter.set(parameter.get() > 0.5 ? 0 : 127, 128);
            }
            else if (0 <= channel && channel < 6 && data1 == 67 && data2 == 127)
               mMainTrackBank.getChannel(channel).selectInMixer();
            else if (0 <= channel && channel < 6 && data1 == 68 && data2 == 127)
               mMainTrackBank.getChannel(channel).stop();
            else if (0 <= channel && channel < 6 && data1 == 69 && data2 == 127)
               mMainTrackBank.getChannel(channel).clipLauncherSlotBank().launch(0);
            else if (channel == 12 && data1 == 87 && data2 == 127)
               mMainTrackBank.sceneBank().getItemAt(0).launch();
            else if (channel == 12 && data1 == 90 && data2 == 127)
               mMainTrackBank.sceneBank().scrollBackwards();
            else if (channel == 12 && data1 == 89 && data2 == 127)
               mMainTrackBank.sceneBank().scrollForwards();
            else if (channel == 12 && 78 <= data1 && data1 <= 80 && data2 == 127)
               mEffectTrackBank.getItemAt(data1 - 78).getMute().toggle();
            break;
      }
   }
}
