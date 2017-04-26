package com.mathieupe;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class VCM600ExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID DRIVER_ID = UUID.fromString("64a5748d-6d00-4e56-b2f0-019a16bd9fc5");
   
   public VCM600ExtensionDefinition()
   {
   }

   @Override
   public String getName()
   {
      return "VCM-600";
   }
   
   @Override
   public String getAuthor()
   {
      return "Matjö";
   }

   @Override
   public String getVersion()
   {
      return "1.0";
   }

   @Override
   public UUID getId()
   {
      return DRIVER_ID;
   }
   
   @Override
   public String getHardwareVendor()
   {
      return "Vestax";
   }
   
   @Override
   public String getHardwareModel()
   {
      return "VCM-600";
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 3;
   }

   @Override
   public boolean shouldFailOnDeprecatedUse()
   {
      return true;
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 1;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 1;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      if (platformType == PlatformType.WINDOWS)
      {
         list.add(new String[]{"VCM-600"}, new String[]{"VCM-600"});
      }
      else if (platformType == PlatformType.MAC)
      {
         list.add(new String[]{"VCM-600"}, new String[]{"VCM-600"});
      }
      else if (platformType == PlatformType.LINUX)
      {
         list.add(new String[]{"VCM-600 MIDI 1"}, new String[]{"VCM-600 MIDI 1"});
      }
   }

   @Override
   public VCM600Extension createInstance(final ControllerHost host)
   {
      return new VCM600Extension(this, host);
   }
}
