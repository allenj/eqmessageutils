Cube Format
===========

The QDDS system uses
formats from the CUBE system. The currently used formats are:


# E - Earthquake

The E format messages are used to add an earthquake to the catalog or
modify the summary information about and earthquake by issuing a new version.
The highest version or most recently received information of the same version
is used. The format is:

```
TpEidnumbrSoVYearMoDyHrMnSecLatddddLongddddDeptMgNstNphDminRmssErhoErzzGpMNmEmLC
12345678901234567890123456789012345678901234567890123456789012345678901234567890
         1         2         3         4         5         6         7         8

a2 * Tp   = Message type = "E " (seismic event)
a8 * Eid  = Event identification number  (any string)
a2 * So   = Data Source =  regional network designation
a1 * V    = Event Version     (ASCII char, except [,])
i4 * Year = Calendar year                (GMT) (-999-6070)
i2 * Mo   = Month of the year            (GMT) (1-12)
i2 * Dy   = Day of the month             (GMT) (1-31)
i2 * Hr   = Hours since midnight         (GMT) (0-23)
i2 * Mn   = Minutes past the hour        (GMT) (0-59)
i3 * Sec  = Seconds past the minute * 10 (GMT) (0-599)
i7 * Lat  = Latitude:  signed decimal degrees*10000 north&gt;0
i8 * Long = Longitude: signed decimal degrees*10000 west &lt;0
i4   Dept = Depth below sea level, kilometers * 10
i2   Mg   = Magnitude * 10
i3   Nst  = Number of stations used for location
i3   Nph  = Number of phases used for location
i4   Dmin = Distance to 1st station;   kilometers * 10
i4   Rmss = Rms time error; sec * 100
i4   Erho = Horizontal standard error; kilometers * 10
i4   Erzz = Vertical standard error;   kilometers * 10
i2   Gp   = Azimuthal gap, percent of circle; degrees/3.6
a1   M    = Magnitude type
i2   Nm   = Number of stations for magnitude determination
i2   Em   = Standard error of the magnitude * 10
a1   L    = Location method
a1 * C    = Menlo Park check character, defined below

"Message Type" field:
     The second character, following the 'E', is no
     longer used for magnitude validation.  Prior "En"
     format that used the 2nd char for magnitude
     validation is no longer supported.

"Event Identification Number" field:
     Any string, excluding '[' and ']' characters.
     Must be a unique identifier for the event.
     Two events with the same identifier from the same
     source will be the same event.  Format varies by
     source - either numeric or alpha-numeric.

"Data Source" field: regional seismic network designation:
     See http://www.iris.washington.edu/FDSN/networks.txt

"Version Number" field:
     May have any value (ASCII 32-126, except 91 or 93).
     Meaning varies by source. Used to distinguish between
     different versions of the same event.

"Location Method" field: varies by source (in parentheses):
     Upper-case indicates an unconfirmed event,
     Lower-case indicates event is confirmed by human review

     A = Binder (AK)
     D = Antelope (NN)
     F = nonNEIC-furnished (US)
     H = Hypoinverse (CI,UU,UW)
     L = Earthworm "local" event (NC)
     M = macroseismic or "felt" (US)
     R = NEIC-furnished (US)

"Magnitude Type" field:
     B = body magnitude (Mb)
     C = duration magnitude (Md)
     D = duration magnitude (Md)
     E = energy magnitude (Me)
     G = local magnitude (Ml)
     I = "Tsuboi" moment magnitude (Mi)
     L = local magnitude (Ml)
     N = "Nuttli" surface wave magnitude (MbLg)
     O = moment magnitude (Mw)
     P = body magnitude (Mb)
     S = surface wave magnitude (Ms)
     T = teleseismic moment magnitude (Mt)
     W = regional moment magnitude (Mw)

"Menlo Park Check Character" field:
     Menlo-Park checksum, calculated 1st through 79th
     char in the message.  Checksum method defined by
     C language source code, below.
     NB: Square bracket characters ARE ACCEPTED in this
         field.

/*-------------------------------------------------------
*  Menlo Park(USGS) check char (Nov.94) cleaned up by PTG
*  Argument:  pch = null terminated string
*  Returns:   Menlo Park check char, excess 36 modulo 91
*/
int MenloCheckChar( char* pch )
{
     unsigned short sum;

     for( sum=0; *pch; pch++ )
         sum = ((sum&amp;01)?0x8000:0) + (sum&gt;&gt;1) + *pch;

     return (int)(36+sum%91);
}

Examples:
E 09082344CI21999040217051050339860-1169945017316000014001800120009004332C0002hP
E meav    US3199904021838195-201884 1681247 33054 19 192283 062 387  00  B 8   v
```


# DE - Delete Earthquake
The DE format is used to delete an earthquake. A delete message deletes
the same, and all lower, versions of an earthquake. The format is:

```
TpEidnumbrScV message........ to 80 characters
123456789012345678901234567890
         1         2         3

a2 * Tp  = Message type = "DE" = delete seismic event
a8 * Eid = Event identification number of event to delete
a2 * Sc  = Data Source
a1 * V   = Version. If version is blank, the version of the latest E message at the
	time the message arrived is assumed, and all current versions are deleted.
	Subsequent submission of E messages with higher versions are not deleted.
a    ... = Optional text message

Event identification and data source must be the same as
specified for "E " message (format 2, above).  The
identified event will be deleted by the receiver.

Example:
DE09081845CI2 EVENT CANCELLED:  (LKH)

Note: when stored in the catalog directory a 20 column time stamp is put at the
beginning of the line.
```


# LI - Link to Addon

The LI format is used to issue a link to additional information (addons)
about an event that are available on some accessible web site. Addons can be
updated by using higher version numbers. The file name gives the data source,
the eventid, the version number, the addon type, and is followed by .add or
.del (if it is a delete addon message). The format is:

```
TpEidnumbrScVV message........ must be on one line.
123456789012345678901234567890
         1         2         3

a2 * Tp =  Message type = "LI" text message
a8 * Eid = Event identification number of event to delete
a2 * Sc  = Data Source
a2 * VV  = Version # of the addon comment.
a    ... = message contains three strings separated by spaces:
           1. a string that defines the type of addon, this string should be unique amoung
              the addons being used.  See the recenteqs system for more information.
           2. the url where the addon information is available
           3. the text that will be used to describe what is available at the url.
              This third string only may contain spaces.  If the text is "delete" the
              addon is deleted.

Example:
LI 006729 NC01 fm http://whatever/whoknows This is a test

Example as stored in eqaddons/nc006729.01.fm.add:
::::::::::::::
event addon type fm version 01 issued at 1999/03/31_20:51:41:
"http://whatever/whoknows""This is a test"

Example:
LI 006729 NC01 fm http://whatever/whoknows delete:

Example as stored in eqaddons/nc006729.01.fm.del:
event addon type fm version 01 issued at 1999/03/31_21:00:17:
"http://whatever/whoknows""delete"

```
