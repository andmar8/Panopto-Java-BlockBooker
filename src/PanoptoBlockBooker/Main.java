    /*
     * This file is part of Panopto-Java-BlockBooker.
     * 
     * Panopto-Java-BlockBooker is free software: you can redistribute it and/or modify
     * it under the terms of the GNU General Public License as published by
     * the Free Software Foundation, either version 3 of the License, or
     * (at your option) any later version.
     * 
     * Panopto-Java-BlockBooker is distributed in the hope that it will be useful,
     * but WITHOUT ANY WARRANTY; without even the implied warranty of
     * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     * GNU General Public License for more details.
     * 
     * You should have received a copy of the GNU General Public License
     * along with Panopto-Java-BlockBooker.  If not, see <http://www.gnu.org/licenses/>.
     * 
     * Copyright: Andrew Martin, Newcastle University
     * 
     */

package PanoptoBlockBooker;

import com.panopto.remoterecorder.RemoteRecorderManagementStub;
import com.panopto.remoterecorder.RemoteRecorderManagementStub.ArrayOfguid;
import com.panopto.remoterecorder.RemoteRecorderManagementStub.Guid;
import com.panopto.remoterecorder.RemoteRecorderManagementStub.ScheduleRecording;
import com.panopto.session.SessionManagementStub;
import com.panopto.session.SessionManagementStub.GetSessionsList;
import com.panopto.session.SessionManagementStub.GetSessionsListResponse;
import com.panopto.session.SessionManagementStub.ListSessionsRequest;
import com.panopto.session.SessionManagementStub.UpdateSessionExternalId;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import uk.ac.ncl.panopto.CSVParser;
import uk.ac.ncl.panopto.PanoptoSessionStringListParser;

public class Main
{
    public static void main(String[] args)
    {
        //Carriage returns in the middle of quoted data needs to be stripped/\n'ed/<br/>'ed
        if(args.length!=4 || args[0].equalsIgnoreCase("--help") || args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("-help") || 
           args[0].equalsIgnoreCase("--h") || args[0].equalsIgnoreCase("/?"))
        {
            System.out.println("\nUsage: java -jar PanoptoBlockBooker.jar <Server>" +
                                                                    "\n\t\t\t\t\t<Username>" +
                                                                    "\n\t\t\t\t\t<Password>" +
                                                                    "\n\t\t\t\t\t<.csv file>");
            System.out.println("\nExample: java -jar PanoptoBlockBooker.jar panoptowfe.ncl.ac.uk admin password sessions.csv\n");
            return;
        }
        new Main().blockBook(args[0],args[1],args[2],args[3]/*,args.length>=4?args[3]:"false",args.length==5?args[4]:""*/);
    }

    public void blockBook(String server, String username, String password, String inputFile/*, String authnReqArg, String authnTypeArg*/)
    {
        try
        {
            RemoteRecorderManagementStub rrmStub = new RemoteRecorderManagementStub("https://"+server+"/Panopto/PublicAPISSL/4.2/RemoteRecorderManagement.svc");
            RemoteRecorderManagementStub.AuthenticationInfo rrmAuth = new RemoteRecorderManagementStub.AuthenticationInfo();
            rrmAuth.setUserKey(username);
            rrmAuth.setPassword(password);

            SessionManagementStub smStub = new SessionManagementStub("https://"+server+"/Panopto/PublicAPISSL/4.2/SessionManagement.svc");
            SessionManagementStub.AuthenticationInfo smAuth = new SessionManagementStub.AuthenticationInfo();
            smAuth.setUserKey(username);
            smAuth.setPassword(password);
            
            this.bookSchedules(smAuth, smStub, rrmStub,this.parseStringListToScheduleRecordings(rrmStub, rrmAuth, smStub, smAuth, this.parseCSVToStringList(inputFile)));
        }
        catch(Exception e)
        {
            System.err.println(e.getMessage());
        }
    }

    private List<List<String>> parseCSVToStringList(String inputFile) throws Exception
    {
        try
        {
            return new CSVParser(inputFile).parseCSV();
        }
        catch(FileNotFoundException fnfe)
        {
            throw new Exception(", the file could not be found");
        }
        catch(IOException ioe)
        {
            throw new Exception(", error whilst reading file: "+ioe.getMessage());
        }
    }

    private Map<String,List<ScheduleRecording>> parseStringListToScheduleRecordings(RemoteRecorderManagementStub rrmStub, 
            RemoteRecorderManagementStub.AuthenticationInfo rrmAuth, SessionManagementStub smStub, 
            SessionManagementStub.AuthenticationInfo smAuth, List<List<String>> parsedSchedules) throws Exception
    {
        return PanoptoSessionStringListParser.parse(rrmStub, rrmAuth, smStub, smAuth, parsedSchedules);
    }
    
    private void bookSchedules(SessionManagementStub.AuthenticationInfo smAuth, SessionManagementStub smStub, 
            RemoteRecorderManagementStub stub, Map<String, List<ScheduleRecording>> scheduleRecordings)
    {
        if(scheduleRecordings.size()>0)
        {
            ScheduleRecording sr = null;
            Iterator<Entry<String, List<ScheduleRecording>>> sri = scheduleRecordings.entrySet().iterator();
            while(sri.hasNext())
            {
                try
                {
                    // the structure here needed to be changed around so that the value and the key go into seperate variables
                    // from there they can be used by different methods/objects
                    // might have to add an iterator to the values object... 
                    // 
                    Entry<String, List<ScheduleRecording>> map = sri.next(); 
                    List<ScheduleRecording> recordings = map.getValue(); 
                    String name = map.getKey(); 
                    UpdateSessionExternalId usei = new UpdateSessionExternalId();
                    Iterator <ScheduleRecording> srI = recordings.iterator(); 
                    
                    while(srI.hasNext()) 
                    {
                        sr = srI.next(); 
                        System.out.print("Attempting to book "+sr.getName());
                        if(sr.getRecorderSettings() == null) {
                            System.out.println(sr.getName() + " ...Error: Can't be booked as the associated recorder cannot be found! ");
                        }
                        
                        com.panopto.remoterecorder.RemoteRecorderManagementStub.ScheduledRecordingResult srr = stub.scheduleRecording(sr).getScheduleRecordingResult(); 
                        
                        if(srr.getConflictsExist())
                        {
                            System.out.println("Error: Session conflict detected! ");
                        } else {
                            ArrayOfguid sessid = srr.getSessionIDs(); 
                            Guid[] guidArray = sessid.getGuid();
                            SessionManagementStub.Guid smGuid =  new SessionManagementStub.Guid(); 
                            smGuid.setGuid(guidArray[0].getGuid()); 
                            usei.setAuth(smAuth);
                            usei.setExternalId(name);
                            usei.setSessionId(smGuid);
                            smStub.updateSessionExternalId(usei);
                            System.out.println("...Booked");
                        }
                    }
                }
                catch(Exception e) 
                {
                    //e.printStackTrace();
                    if(sr.getRecorderSettings() != null) {
                        System.out.println("Could not book schedule: "+sr.getName());
                        System.out.println(e.getMessage());
                    }
                }
            }
        }
        else
        {
            System.out.println("No valid schedules found");
        }
        System.out.println("All done.");
    }
}
