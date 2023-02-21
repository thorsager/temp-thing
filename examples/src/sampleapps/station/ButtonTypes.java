/*
 * ButtonTypes.java
 * 
 * Copyright (c) 2007 Avaya Inc. All rights reserved.
 * 
 * USE OR INSTALLATION OF THIS SAMPLE DEMONSTRATION SOFTWARE INDICATES THE END
 * USERS ACCEPTANCE OF THE GENERAL LICENSE TERMS AVAILABLE ON THE AVAYA WEBSITE
 * AT http://support.avaya.com/LicenseInfo/ (GENERAL LICENSE TERMS). DO NOT USE
 * THE SOFTWARE IF YOU DO NOT WISH TO BE BOUND BY THE GENERAL LICENSE TERMS. IN
 * ADDITION TO THE GENERAL LICENSE TERMS, THE FOLLOWING ADDITIONAL TERMS AND
 * RESTRICTIONS WILL TAKE PRECEDENCE AND APPLY TO THIS DEMONSTRATION SOFTWARE.
 * 
 * THIS DEMONSTRATION SOFTWARE IS PROVIDED FOR THE SOLE PURPOSE OF DEMONSTRATING
 * HOW TO USE THE SOFTWARE DEVELOPMENT KIT AND MAY NOT BE USED IN A LIVE OR
 * PRODUCTION ENVIRONMENT. THIS DEMONSTRATION SOFTWARE IS PROVIDED ON AN AS IS
 * BASIS, WITHOUT ANY WARRANTIES OR REPRESENTATIONS EXPRESS, IMPLIED, OR
 * STATUTORY, INCLUDING WITHOUT LIMITATION, WARRANTIES OF QUALITY, PERFORMANCE,
 * INFRINGEMENT, MERCHANTABILITY, OR FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * EXCEPT FOR PERSONAL INJURY CLAIMS, WILLFUL MISCONDUCT AND END USERS VIOLATION
 * OF AVAYA OR ITS SUPPLIERS INTELLECTUAL PROPERTY RIGHTS, INCLUDING THROUGH A
 * BREACH OF THE SOFTWARE LICENSE, NEITHER AVAYA, ITS SUPPLIERS NOR END USER
 * SHALL BE LIABLE FOR (i) ANY INCIDENTAL, SPECIAL, STATUTORY, INDIRECT OR
 * CONSEQUENTIAL DAMAGES, OR FOR ANY LOSS OF PROFITS, REVENUE, OR DATA, TOLL
 * FRAUD, OR COST OF COVER AND (ii) DIRECT DAMAGES ARISING UNDER THIS AGREEMENT
 * IN EXCESS OF FIFTY DOLLARS (U.S. $50.00).
 * 
 * To the extent there is a conflict between the General License Terms, your
 * Customer Sales Agreement and the terms and restrictions set forth herein, the
 * terms and restrictions set forth herein shall prevail solely for this Utility
 * Demonstration Software.
 */

//***************************************************************
//* package
//***************************************************************

package sampleapps.station;

//***************************************************************
//* imports
//***************************************************************

import java.util.HashMap;
import java.util.Iterator;

//***************************************************************
//* ButtonTypes
//***************************************************************

/**
A Type-safe enumeration for ButtonTypes.
**/

public class ButtonTypes {

//***************************************************************
//* Constructors
//***************************************************************

  private ButtonTypes(int typeID, String typeName) {
    this.typeID = typeID;
    this.typeName = typeName;
    hash.put(new Integer(typeID), this);
  }

//***************************************************************
//* typeID Methods
//***************************************************************

  /** returns the enum value as an integer **/
  public int getID() { return typeID; }

  /** returns the enum value as a name **/
  public String getName() { return typeName; }

  /** returns the type associated with the given int **/
  public static ButtonTypes getButtonTypes(int id) throws IllegalArgumentException {
    if(hash.containsKey(new Integer(id)))
        return hash.get(new Integer(id));
    else
        throw new IllegalArgumentException();
  }

//***************************************************************
//* Enumeration Methods
//***************************************************************

  /** returns an iterator that can be used to iterate through
      this enumeration's range **/
  @SuppressWarnings("unchecked")
  public static Iterator<ButtonTypes> iterator() {
    HashMap<Integer, ButtonTypes> clone = (HashMap<Integer, ButtonTypes>)hash.clone();
    return clone.values().iterator();
  }

//***************************************************************
//* Members
//***************************************************************

  // Keep a list of EnumerationTypes so that we can
  // Enumerate them, etc.
  private static HashMap<Integer, ButtonTypes> hash = new HashMap<Integer, ButtonTypes>();

  private int typeID;
  private String typeName;

//***************************************************************
//* Types
//***************************************************************

  // This allows the ButtonTypes to be type-safe, it also
  // ensures that no outside classes can create a new
  // ButtonTypes (note the private constructor.
  /**  **/
  public static final ButtonTypes Drop = new ButtonTypes(1, "Drop");
  /**  **/
  public static final ButtonTypes Conf = new ButtonTypes(2, "Conf");
  /**  **/
  public static final ButtonTypes Transfer = new ButtonTypes(3, "Transfer");
  /**  **/
  public static final ButtonTypes Hold = new ButtonTypes(4, "Hold");
  /**  **/
  public static final ButtonTypes MsgWaiting = new ButtonTypes(5, "MsgWaiting");
  /**  **/
  public static final ButtonTypes CallAppr = new ButtonTypes(6, "CallAppr");
  /**  **/
  public static final ButtonTypes LWC_Store = new ButtonTypes(10, "LWC_Store");
  /**  **/
  public static final ButtonTypes Msg_Retr = new ButtonTypes(11, "Msg_Retr");
  /**  **/
  public static final ButtonTypes Cov_Msg_Rt = new ButtonTypes(12, "Cov_Msg_Rt");
  /**  **/
  public static final ButtonTypes Next = new ButtonTypes(13, "Next");
  /**  **/
  public static final ButtonTypes Delete_Msg = new ButtonTypes(14, "Delete_Msg");
  /**  **/
  public static final ButtonTypes Normal = new ButtonTypes(15, "Normal");
  /**  **/
  public static final ButtonTypes Call_Disp = new ButtonTypes(16, "Call_Disp");
  /**  **/
  public static final ButtonTypes Cov_CBack = new ButtonTypes(17, "Cov_CBack");
  /**  **/
  public static final ButtonTypes LWC_Lock = new ButtonTypes(18, "LWC_Lock");
  /**  **/
  public static final ButtonTypes LWC_Cancel = new ButtonTypes(19, "LWC_Cancel");
  /**  **/
  public static final ButtonTypes Cov_LWC_St = new ButtonTypes(20, "Cov_LWC_St");
  /**  **/
  public static final ButtonTypes Inspect = new ButtonTypes(21, "Inspect");
  /**  **/
  public static final ButtonTypes Stored_Num = new ButtonTypes(22, "Stored_Num");
  /**  **/
  public static final ButtonTypes Date_Time = new ButtonTypes(23, "Date_Time");
  /**  **/
  public static final ButtonTypes Timer = new ButtonTypes(24, "Timer");
  /**  **/
  public static final ButtonTypes TestDisp = new ButtonTypes(25, "TestDisp");
  /**  **/
  public static final ButtonTypes Directory = new ButtonTypes(26, "Directory");
  /**  **/
  public static final ButtonTypes Auto_Wkup = new ButtonTypes(27, "Auto_Wkup");
  /**  **/
  public static final ButtonTypes Check_Out = new ButtonTypes(28, "Check_Out");
  /**  **/
  public static final ButtonTypes Check_In = new ButtonTypes(29, "Check_In");
  /**  **/
  public static final ButtonTypes In_Call_Id = new ButtonTypes(30, "In_Call_Id");
  /**  **/
  public static final ButtonTypes Per_COline = new ButtonTypes(31, "Per_COline");
  /**  **/
  public static final ButtonTypes Dial_Icom = new ButtonTypes(32, "Dial_Icom");
  /**  **/
  public static final ButtonTypes Auto_C_Back = new ButtonTypes(33, "Auto_C_Back");
  /**  **/
  public static final ButtonTypes Call_Pkup = new ButtonTypes(34, "Call_Pkup");
  /**  **/
  public static final ButtonTypes Send_Calls = new ButtonTypes(35, "Send_Calls");
  /**  **/
  public static final ButtonTypes Goto_Cover = new ButtonTypes(36, "Goto_Cover");
  /**  **/
  public static final ButtonTypes Signal = new ButtonTypes(37, "Signal");
  /**  **/
  public static final ButtonTypes Man_Msg_Wt = new ButtonTypes(38, "Man_Msg_Wt");
  /**  **/
  public static final ButtonTypes Busy_Ind = new ButtonTypes(39, "Busy_Ind");
  /**  **/
  public static final ButtonTypes Term_X_Gr = new ButtonTypes(40, "Term_X_Gr");
  /**  **/
  public static final ButtonTypes Exclusion = new ButtonTypes(41, "Exclusion");
  /**  **/
  public static final ButtonTypes Consult = new ButtonTypes(42, "Consult");
  /**  **/
  public static final ButtonTypes Data_Ext = new ButtonTypes(43, "Data_Ext");
  /**  **/
  public static final ButtonTypes Music = new ButtonTypes(44, "Music");
  /**  **/
  public static final ButtonTypes Call_Park = new ButtonTypes(45, "Call_Park");
  /**  **/
  public static final ButtonTypes ACTG = new ButtonTypes(46, "ACTG");
  /**  **/
  public static final ButtonTypes DEACTG = new ButtonTypes(47, "DEACTG");
  /**  **/
  public static final ButtonTypes DTGS1 = new ButtonTypes(48, "DTGS1");
  /**  **/
  public static final ButtonTypes DTGS3 = new ButtonTypes(49, "DTGS3");
  /**  **/
  public static final ButtonTypes Release = new ButtonTypes(50, "Release");
  /**  **/
  public static final ButtonTypes Cancel = new ButtonTypes(51, "Cancel");
  /**  **/
  public static final ButtonTypes Aux_Work = new ButtonTypes(52, "Aux_Work");
  /**  **/
  public static final ButtonTypes Night_Serv = new ButtonTypes(53, "Night_Serv");
  /**  **/
  public static final ButtonTypes PosAvail = new ButtonTypes(54, "PosAvail");
  /**  **/
  public static final ButtonTypes Start = new ButtonTypes(55, "Start");
  /**  **/
  public static final ButtonTypes Split = new ButtonTypes(56, "Split");
  /**  **/
  public static final ButtonTypes ForcedRel = new ButtonTypes(57, "ForcedRel");
  /**  **/
  public static final ButtonTypes HundGrp = new ButtonTypes(58, "HundGrp");
  /**  **/
  public static final ButtonTypes COR = new ButtonTypes(59, "COR");
  /**  **/
  public static final ButtonTypes ICR_Cut = new ButtonTypes(60, "ICR_Cut");
  /**  **/
  public static final ButtonTypes RCR_Cut = new ButtonTypes(61, "RCR_Cut");
  /**  **/
  public static final ButtonTypes CWR_Cut = new ButtonTypes(62, "CWR_Cut");
  /**  **/
  public static final ButtonTypes TRK_Id = new ButtonTypes(63, "TRK_Id");
  /**  **/
  public static final ButtonTypes Emergency = new ButtonTypes(64, "Emergency");
  /**  **/
  public static final ButtonTypes Abrv_Dial = new ButtonTypes(65, "Abrv_Dial");
  /**  **/
  public static final ButtonTypes Last_Numb = new ButtonTypes(66, "Last_Numb");
  /**  **/
  public static final ButtonTypes Abr_Prog = new ButtonTypes(67, "Abr_Prog");
  /**  **/
  public static final ButtonTypes Abr_SpChar = new ButtonTypes(68, "Abr_SpChar");
  /**  **/
  public static final ButtonTypes Auto_Icom = new ButtonTypes(69, "Auto_Icom");
  /**  **/
  public static final ButtonTypes Aut_Msg_Wt = new ButtonTypes(70, "Aut_Msg_Wt");
  /**  **/
  public static final ButtonTypes Print_Msgs = new ButtonTypes(71, "Print_Msgs");
  /**  **/
  public static final ButtonTypes Send_Term = new ButtonTypes(72, "Send_Term");
  /**  **/
  public static final ButtonTypes Brdg_Appr = new ButtonTypes(73, "Brdg_Appr");
  /**  **/
  public static final ButtonTypes Call_Fwd = new ButtonTypes(74, "Call_Fwd");
  /**  **/
  public static final ButtonTypes Verify = new ButtonTypes(75, "Verify");
  /**  **/
  public static final ButtonTypes CAS_Back_Up = new ButtonTypes(76, "CAS_Back_Up");
  /**  **/
  public static final ButtonTypes ACA_Halt = new ButtonTypes(77, "ACA_Halt");
  /**  **/
  public static final ButtonTypes RTGS1 = new ButtonTypes(78, "RTGS1");
  /**  **/
  public static final ButtonTypes RTGS3 = new ButtonTypes(79, "RTGS3");
  /**  **/
  public static final ButtonTypes Ringer_Off = new ButtonTypes(80, "Ringer_Off");
  /**  **/
  public static final ButtonTypes Priority = new ButtonTypes(81, "Priority");
  /**  **/
  public static final ButtonTypes Alarm = new ButtonTypes(82, "Alarm");
  /**  **/
  public static final ButtonTypes CFwd_BsyDa = new ButtonTypes(84, "CFwd_BsyDa");
  /**  **/
  public static final ButtonTypes Serv_Obsrv = new ButtonTypes(85, "Serv_Obsrv");
  /**  **/
  public static final ButtonTypes Q_Time = new ButtonTypes(86, "Q_Time");
  /**  **/
  public static final ButtonTypes Q_Calls = new ButtonTypes(87, "Q_Calls");
  /**  **/
  public static final ButtonTypes Atd_Qtime = new ButtonTypes(88, "Atd_Qtime");
  /**  **/
  public static final ButtonTypes Atd_Qcalls = new ButtonTypes(89, "Atd_Qcalls");
  /**  **/
  public static final ButtonTypes Assist = new ButtonTypes(90, "Assist");
  /**  **/
  public static final ButtonTypes After_Call = new ButtonTypes(91, "After_Call");
  /**  **/
  public static final ButtonTypes Auto_In = new ButtonTypes(92, "Auto_In");
  /**  **/
  public static final ButtonTypes Manual_In = new ButtonTypes(93, "Manual_In");
  /**  **/
  public static final ButtonTypes ACD_Release = new ButtonTypes(94, "Release");
  /**  **/
  public static final ButtonTypes Ext_Dn_Dst = new ButtonTypes(95, "Ext_Dn_Dst");
  /**  **/
  public static final ButtonTypes Grp_Dn_Dst = new ButtonTypes(96, "Grp_Dn_Dst");
  /**  **/
  public static final ButtonTypes Mwn_Act = new ButtonTypes(97, "Mwn_Act");
  /**  **/
  public static final ButtonTypes Mwn_Deact = new ButtonTypes(98, "Mwn_Deact");
  /**  **/
  public static final ButtonTypes Dn_Dst = new ButtonTypes(99, "Dn_Dst");
  /**  **/
  public static final ButtonTypes I_Auto_Ans = new ButtonTypes(100, "I_Auto_Ans");
  /**  **/
  public static final ButtonTypes Hunt_Ns = new ButtonTypes(101, "Hunt_Ns");
  /**  **/
  public static final ButtonTypes Trunk_Ns = new ButtonTypes(102, "Trunk_Ns");
  /**  **/
  public static final ButtonTypes Link_Alarm = new ButtonTypes(103, "Link_Alarm");
  /**  **/
  public static final ButtonTypes Major_Alrm = new ButtonTypes(104, "Major_Alrm");
  /**  **/
  public static final ButtonTypes Pms_Alarm = new ButtonTypes(105, "Pms_Alarm");
  /**  **/
  public static final ButtonTypes Cdr1_Alarm = new ButtonTypes(106, "Cdr1_Alarm");
  /**  **/
  public static final ButtonTypes Warn_Alrm = new ButtonTypes(107, "Warn_Alrm");
  /**  **/
  public static final ButtonTypes Int_Aut_An = new ButtonTypes(108, "Int_Aut_An");
  /**  **/
  public static final ButtonTypes Rs_Alert = new ButtonTypes(109, "Rs_Alert");
  /**  **/
  public static final ButtonTypes Flash = new ButtonTypes(110, "Flash");
  /**  **/
  public static final ButtonTypes Trunk_Name = new ButtonTypes(111, "Trunk_Name");
  /**  **/
  public static final ButtonTypes Clk_Overid = new ButtonTypes(112, "Clk_Overid");
  /**  **/
  public static final ButtonTypes Man_Overid = new ButtonTypes(113, "Man_Overid");
  /**  **/
  public static final ButtonTypes Abrdg_Appr = new ButtonTypes(114, "Abrdg_Appr");
  /**  **/
  public static final ButtonTypes Pr_Pms_Alm = new ButtonTypes(115, "Pr_Pms_Alm");
  /**  **/
  public static final ButtonTypes Pr_Awu_Alm = new ButtonTypes(116, "Pr_Awu_Alm");
  /**  **/
  public static final ButtonTypes Cdr2_Alrm = new ButtonTypes(117, "Cdr2_Alrm");
  /**  **/
  public static final ButtonTypes Pr_Sys_Alm = new ButtonTypes(120, "Pr_Sys_Alm");
  /**  **/
  public static final ButtonTypes Trk_Ac_Alm = new ButtonTypes(121, "Trk_Ac_Alm");
  /**  **/
  public static final ButtonTypes Disp_Norm = new ButtonTypes(124, "Disp_Norm");
  /**  **/
  public static final ButtonTypes Scroll = new ButtonTypes(125, "Scroll");
  /**  **/
  public static final ButtonTypes Off_Bd_Alm = new ButtonTypes(126, "Off_Bd_Alm");
  /**  **/
  public static final ButtonTypes AC_Alarm = new ButtonTypes(128, "AC_Alarm");
  /**  **/
  public static final ButtonTypes Stroke_Cnt = new ButtonTypes(129, "Stroke_Cnt");
  /**  **/
  public static final ButtonTypes MMI_Cp_Alm = new ButtonTypes(132, "MMI_Cp_Alm");
  /**  **/
  public static final ButtonTypes VC_Cp_Alm = new ButtonTypes(133, "VC_Cp_Alm");
  /**  **/
  public static final ButtonTypes Account = new ButtonTypes(134, "Account");
  /**  **/
  public static final ButtonTypes Grp_Page = new ButtonTypes(135, "Grp_Page");
  /**  **/
  public static final ButtonTypes Whisp_Act = new ButtonTypes(136, "Whisp_Act");
  /**  **/
  public static final ButtonTypes Whisp_Anbk = new ButtonTypes(137, "Whisp_Anbk");
  /**  **/
  public static final ButtonTypes Whisp_Off = new ButtonTypes(138, "Whisp_Off");
  /**  **/
  public static final ButtonTypes OCrss_Alert = new ButtonTypes(139, "Crss_Alert");
  /**  **/
  public static final ButtonTypes Work_Code = new ButtonTypes(140, "Work_Code");
  /**  **/
  public static final ButtonTypes CallrInfo = new ButtonTypes(141, "CallrInfo");
  /**  **/
  public static final ButtonTypes Lsvn_Halt = new ButtonTypes(144, "Lsvn_Halt");
  /**  **/
  public static final ButtonTypes Rsvn_Halt = new ButtonTypes(145, "Rsvn_Halt");
  /**  **/
  public static final ButtonTypes Ani_Requst = new ButtonTypes(146, "Ani_Requst");
  /**  **/
  public static final ButtonTypes VIP_Wakeup = new ButtonTypes(147, "VIP_Wakeup");
  /**  **/
  public static final ButtonTypes VIP_Retry = new ButtonTypes(148, "VIP_Retry");
  /**  **/
  public static final ButtonTypes Admin = new ButtonTypes(150, "Admin");
  /**  **/
  public static final ButtonTypes Btn_View = new ButtonTypes(151, "Btn_View");
  /**  **/
  public static final ButtonTypes Softkey1 = new ButtonTypes(152, "Softkey1");
  /**  **/
  public static final ButtonTypes Softkey2 = new ButtonTypes(153, "Softkey2");
  /**  **/
  public static final ButtonTypes Softkey3 = new ButtonTypes(154, "Softkey3");
  /**  **/
  public static final ButtonTypes Softkey4 = new ButtonTypes(155, "Softkey4");
  /**  **/
  public static final ButtonTypes Menu_Sk = new ButtonTypes(156, "Menu");
  /**  **/
  public static final ButtonTypes Exit_Sk = new ButtonTypes(157, "Exit");
  /**  **/
  public static final ButtonTypes Previous_Sk = new ButtonTypes(158, "Previous");
  /**  **/
  public static final ButtonTypes Next_Sk = new ButtonTypes(159, "Next");
  /**  **/
  public static final ButtonTypes MCT_Act = new ButtonTypes(160, "MCT_Act");
  /**  **/
  public static final ButtonTypes MCT_Ctrl = new ButtonTypes(161, "MCT_Ctrl");
  /**  **/
  public static final ButtonTypes Alt_FRL = new ButtonTypes(162, "Alt_FRL");
  /**  **/
  public static final ButtonTypes Debug = new ButtonTypes(163, "Debug");
  /**  **/
  public static final ButtonTypes CPN_Blk = new ButtonTypes(164, "CPN_Blk");
  /**  **/
  public static final ButtonTypes CPN_Unblk = new ButtonTypes(165, "CPN_Unblk");
  /**  **/
  public static final ButtonTypes MM_PCAudio = new ButtonTypes(166, "MM_PCAudio");
  /**  **/
  public static final ButtonTypes MM_Call = new ButtonTypes(167, "MM_Call");
  /**  **/
  public static final ButtonTypes MM_DataCnf = new ButtonTypes(168, "MM_DataCnf");
  /**  **/
  public static final ButtonTypes MM_Basic = new ButtonTypes(169, "MM_Basic");
  /**  **/
  public static final ButtonTypes MM_MultNbr = new ButtonTypes(170, "MM_MultNbr");
  /**  **/
  public static final ButtonTypes Display = new ButtonTypes(180, "Display");
  /**  **/
  public static final ButtonTypes PCORR = new ButtonTypes(186, "PCORR");
  /**  **/
  public static final ButtonTypes NoAns_Alrt = new ButtonTypes(192, "NoAns_Alrt");
  /**  **/
  public static final ButtonTypes VOA_Repeat = new ButtonTypes(208, "VOA_Repeat");
  /**  **/
  public static final ButtonTypes VU_Display = new ButtonTypes(211, "VU_Display");
  /**  **/
  public static final ButtonTypes Group_Disp = new ButtonTypes(212, "Group_Disp");
  /**  **/
  public static final ButtonTypes Grp_Select = new ButtonTypes(213, "Grp_Select");
  /**  **/
  public static final ButtonTypes ASVN_Halt = new ButtonTypes(214, "ASVN_Halt");
  /**  **/
  public static final ButtonTypes Type = new ButtonTypes(224, "Type");
  /**  **/
  public static final ButtonTypes Alrt_AgChg = new ButtonTypes(225, "Alrt_AgChg");
  /**  **/
  public static final ButtonTypes Abrv_Ring = new ButtonTypes(226, "Abrv_Ring");
  /**  **/
  public static final ButtonTypes Autodial = new ButtonTypes(227, "Autodial");
  /**  **/
  public static final ButtonTypes UUI_Info = new ButtonTypes(228, "UUI_Info");
  /**  **/
  public static final ButtonTypes Mf_Op_Intl = new ButtonTypes(229, "Mf_Op_Intl");
  /**  **/
  public static final ButtonTypes Dir_Pkup = new ButtonTypes(230, "Dir_Pkup");
  /**  **/
  public static final ButtonTypes SSVN_Halt = new ButtonTypes(231, "SSVN_Halt");
  /**  **/
  public static final ButtonTypes Disp_Chrg = new ButtonTypes(232, "Disp_Chrg");
  /**  **/
  public static final ButtonTypes Usr_AddBsy = new ButtonTypes(239, "USR_AddBsy");
  /**  **/
  public static final ButtonTypes Usr_RemBsy = new ButtonTypes(240, "Usr_RemBsy");
  /**  **/
  public static final ButtonTypes Headset = new ButtonTypes(241, "Headset");
  /**  **/
  public static final ButtonTypes Call_Timer = new ButtonTypes(243, "Call_Timer");
  /**  **/
  public static final ButtonTypes MM_CFwd = new ButtonTypes(244, "MM_CFwd");
  /**  **/
  public static final ButtonTypes Mf_Da_Intl = new ButtonTypes(246, "Mf_Da_Intl");
  /**  **/
  public static final ButtonTypes Crss_Alert = new ButtonTypes(247, "Crss_Alert");
  /**  **/
  public static final ButtonTypes Did_View = new ButtonTypes(256, "Did_View");
  /**  **/
  public static final ButtonTypes Start_Bill = new ButtonTypes(257, "Start_Bill");
  /**  **/
  public static final ButtonTypes Btn_Ring = new ButtonTypes(258, "Btn_Ring");
  /**  **/
  public static final ButtonTypes Ring_Stat = new ButtonTypes(259, "Ring_Stat");
  /**  **/
  public static final ButtonTypes Did_Remove = new ButtonTypes(276, "Did_Remove");
  /**  **/
  public static final ButtonTypes VIP_ChkIn = new ButtonTypes(277, "VIP_ChkIn");
  /**  **/
  public static final ButtonTypes Call_Scrn = new ButtonTypes(285, "Call_Scrn");
  /**  **/
  public static final ButtonTypes Nh_Consult = new ButtonTypes(290, "Nh_Consult");
  /**  **/
  public static final ButtonTypes Sta_Lock = new ButtonTypes(300, "Sta_Lock");
  /**  **/
  public static final ButtonTypes Audix_Rec = new ButtonTypes(301, "Audix_Rec");
  /**  **/
  public static final ButtonTypes Limit_Call = new ButtonTypes(302, "Limit_Call");
  /**  **/
  public static final ButtonTypes Logout_Ovr = new ButtonTypes(303, "Logout_Ovr");
  /**  **/
  public static final ButtonTypes CFwd_Enh = new ButtonTypes(304, "CFwd_Enh");
  /**  **/
  public static final ButtonTypes Team = new ButtonTypes(305, "Team");
  /**  **/
  public static final ButtonTypes Lic_Error = new ButtonTypes(312, "Lic_Error");
  /**  **/
  public static final ButtonTypes Conf_Dsp = new ButtonTypes(325, "Conf_Dsp");
  /**  **/
  public static final ButtonTypes Voice_Mail = new ButtonTypes(326, "Voice_Mail");
  /**  **/
  public static final ButtonTypes Togle_Swap = new ButtonTypes(327, "Togle_Swap");
  /**  **/
  public static final ButtonTypes FE_Mute = new ButtonTypes(328, "FE_Mute");
  /**  **/
  public static final ButtonTypes Pg1_Alarm = new ButtonTypes(329, "Pg1_Alarm");
  /**  **/
  public static final ButtonTypes Pg2_Alarm = new ButtonTypes(330, "Pg2_Alarm");
  /**  **/
  public static final ButtonTypes Share_Talk = new ButtonTypes(331, "ShareTalk");
  /**  **/
  public static final ButtonTypes EC500 = new ButtonTypes(335, "EC500");
  /**  **/
  public static final ButtonTypes Post_Msgs = new ButtonTypes(336, "Post_Msgs");
  /**  **/
  public static final ButtonTypes Extnd_Call = new ButtonTypes(345, "Extnd_Call");
  /**  **/
  public static final ButtonTypes No_Hld_Cnf = new ButtonTypes(337, "No_Hld_Cnf");
}
