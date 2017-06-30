CityPay PCI PAN Scanner
===

CityPay's PAN scanner is a Scala/Java tool for searching locations for the existence of sensitive primary card holder 
data. 
 
The scanner is able to scan multiple sources such as
  - Multiple Operating Systems including Microsoft &reg; Windows 32/64, Mac OSX 32/64, Linux 32/64, Arm 
  - Local Filesystems
  - Databases (via JDBC drivers such as MySql, SQLServer, Oracle etc)
  - WebSites (Future Planned)
  - Archives, catering for compression streams such as `7-Zip`, `Zip`, `Rar`, `Tar`, `
      Split`, `Lzma`, `Iso`, `HFS`, `GZip`, `Cpio`, `BZip2`, `Z`, `Arj`, `Chm`, `Lhz`, `Cab`, `Nsis`, `Deb`, `Rpm`

*Compliance Information*
  
  Note that all card holder data including PANs within unit tests supplied with this source code
  are *test data* and generated with a valid luhn checksum.
  
## Configuration Options

Configuration is found in 3 core logging files `search.conf`, `chd.conf` and `scanner.conf` 

### Search Configuration

| Parameter | Data Type | Description |
|-----------|-----------|-------------|
| search    | `object`    | holder for search configuration | 
| search.source    | `object`    | holder for source configurations |
| type | `string` | The type of search based on `ScanSourceConfigFactory` such as `file`, `db` |

#### File Searching

| Parameter | Data Type | Description |
|-----------|-----------|-------------|
| type | `string` | "file" |
| root | `string[]` | array of root directories to search  |
| includeHiddenFiles | `boolean` | whether hidden files are scanned. Hidden files are either marked as _hidden_ by the file system or start with "." |
| recursive | `boolean` | whether to recursively scan a root directory. Default value is `true` |
| maxDepth | `int` | the maximum number of directories to scan into |

#### Database Searching

Database searching requires a valid driver which must be provided in the classpath 

| Parameter | Data Type | Description |
|-----------|-----------|-------------|
| type | `string` | "db" |
| driver | `string` | The class name of the jdbc driver to use i.e. "com.mysql.jdbc.Driver" |
| url | `string` | The connection url string to use to provide a connection to the database, Consult the driver documentation for details of examples and configuration of connection strings |
| schema | `string` | The database schema to connect to |
| tableNameRegex | `string` | A regular expression of the table names to scan. This will be dependant on your database setup |
| colNameRegex | `string` | A regular expression of column names to scan |
| credentials | `string` | A class implementation of a `com.citypay.pan.search.util.CredentialsCallback` preconfigured examples are `com.citypay.pan.search.util.NoOpCredentials` which does not provide credentials that may be previously supplied in the connection url. `com.citypay.pan.search.util.CommandLineCredentials` provide a callback function to the command line to enter the username and password at the time of the scan. |



### Scanner Configuration

| Parameter | Data Type | Description |
|-----------|-----------|-------------|
| analaysisBufferSz | `int` | The size of the buffer to use when reading from a file, defaults to 16384 |
| concurrentScans | `int` | The number of concurrent scan processes to run. A scan process is a search configuration such as file system search, database search etc |
| scanArchives | `boolean` | Whether to scan archive files by expanding them for analysis in RAM or temp files.  |
| scanWorkers | `int` | The number of concurrent file scans to perform within a concurrent scan.  |
| stopOnFirstMatch | `boolean` | If a value is found the search will exit the file, reporting that an item was found.  Any further items will not be listed. If the value is false, the entire file will be searched and all items reported |
| renderer | `string` | A renderer to produce a report by, defaults to "json" |
| prettify | `boolean` | A boolean value whether the renderer should pretty print which is easier for human reading |


### Card Holder Data Configuration

| Parameter | Data Type | Description |
|-----------|-----------|-------------|
| chd.level1 | `chd object` | Contains an array of chd specs for analysis at a level 1 stage. Should be short digits for fast searching. Recommended as 1 or 2 digits |
| chd.level2 | `chd object` | Contains an array of chd specs for analysis at a level 2 stage. Should be longer than level1 by 1 or more positions. The more accurate these values, the less false positives are found |

CHD Specs

| Parameter | Data Type | Description |
|-----------|-----------|-------------|
| name | `string `| The name of the card scheme spec |
| id | `string `| An id of the spec to identify by, will group via the id when testing for schemes |
| logo | `string `| The name of a logo file associated with the scheme |
| len | `string `| The length of the scheme provided as a single value of a range of values specified with a dash such as 16 or 16-19 |
| bins | `int[]` | An array of ints that form the leading values for analysis |

 

## Scanning Algorithm

The scanner uses an algorithm to assess whether the file contains sensitive data with the purpose 
to scan against multiple character sets and encrypted files. 

 ### Character Definition
- Scan characters are defined as numeric groups (*N*) `[\u0030-\u0039]` with delimiters of `[\u0020\u002D\u0009]` 
  being space, comma and tab. In _PAN_ storage, this would find examples such as `4000000000000002`, 
  `4000-0000-0000-0002`, `4000 0000 0000 0002`. A combination of both characters are labelled as _D_

### Input Stream analysis
  
  Data is collated and analysed from an input stream buffer of `analysisBufferSz` bytes. If a `N` digit is found it is
  appended to the inspection buffer and its index position marked. If the next character is "out of bounds" being a non
  _D_ character, the stream is reset. Should the next character match it is appended and the overall length of the 
   inspection buffer vs the minimum pan length is checked. If we are in an inspection range, we move to an inspection
   process.  This manner of providing multiple buffers aids in preventing over analysis of data.
  
### PAN Inspection

Pan inspection analyses the incoming bytes and collates bytes based on matching characters in _D_ until a non _D_ 
character occurs or a buffer limit is obtained. In a data file it is expected that delimiters such as ',\n' etc will 
divide items accordingly, to prevent overflows a buffer limit is included.

Once enough data is buffered for analysis, we use a modified version of the Knuth Morris Pratt algorithm 
([KMP](https://en.wikipedia.org/wiki/Knuth%E2%80%93Morris%E2%80%93Pratt_algorithm)).

An example is to run the algorithm as `W` = "45NNNNNNNNNNNNNN" where N and `S` = "4514500000000000000000012325",
_N_ is a numeric value with a length of 16.
- `m`, denoting the position within `S` where the prospective match for `W` begins,
- `i`, denoting the index of the currently considered character in `W`

In each step the algorithm compares `S[m+i]` with `W[i]` and increments `i` if they are equal. This is illustrated at
the start, such as:

```
             1         2            3         4
m: 0123456789012345678901201234567890123456789012
S: 4514500000000000000000012325
W: 45NNNNNNNNNNNNNN
i: 0123456789012345
                  L
```

At the point that the required number of digits are found, a luhn check is performed to confirm that it is indeed
a card number. Should the luhn check fail, the algorithm either continues (if there is a range in length i.e. 16-19)


```
             1         2            3         4
m: 0123456789012345678901201234567890123456789012
S: 4514500000000000000000012325
W: 45NNNNNNNNNNNNNNNNN
i: 0123456789012345678
                   LLL
```
  
If no match is found, it searches from the next instance, where `m = 3`.   
  
```
             1         2            3         4
m: 0123456789012345678901201234567890123456789012
S: 4514500000000000000000012325
W:    45NNNNNNNNNNNNNN
i:    0123456789012345
                     L
```  

For further information see
  
1. https://en.wikipedia.org/wiki/Knuth%E2%80%93Morris%E2%80%93Pratt_algorithm
2. http://algs4.cs.princeton.edu/53substring/KMP.java.html
  
## Troubleshooting
  
  The scanner provides full trace logs for analysis of its processes and can be enabled by providing a java
  system property of `pan.search.trace` when running the scan. It can be run in multiple combinations set
  out in the table below, delimited with a comma or by setting the value to `all` e.g. 
  `java com.citypay.pan.sesarch.Scanner -Dpan.search.trace=all` Any trace logging will be sent to 
  System.out by default.
  
  | Key | Description |
  |-----|-------------|
  | `analysisBuffer` | trace logging for the analysis buffer which is run when searching the buffer for card data. The analysis buffer is run on level 1 pans only |
  | `analysisBufferL2` | trace logging for the abalysis buffer which is run on a level 2 search, after level 1 has found results. |
  | `db` | trace logging for database column searches |
  | `inspectionScanner` | used on the inspection buffer to analysis the inspection routines |
  
## Reporting
  
The scanner outputs a JSON file containing the results of the scan which can be loaded via HTML to construct 
working reports to meet compliance. 