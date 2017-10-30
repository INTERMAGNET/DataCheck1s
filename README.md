# Software project description

|  |  |
|------------------------- | ----------- |
| Project Name:            | DataCheck1s |
| Main developer(s):       | Simon Flower |
| Main user(s):            | Simon Flower (on behalf of INTERMAGNET) |
| Computer language(s):    | Java |
| Development environment: | Netbeans |
| BGS team:                | Geomagnetism |


## Description

A 'stop gap' program to enable INTERMAGNET data checkers to check 1-second data against 1-minute data that has already been through a quality control process. The program also creates 1-second data files in CDF format. Input format is IAF for 1-minute data and IAGA-2002 for 1-second data.

This program is the direct response to an action from the INTERMAGNET meeting in Hyderabad / Niemegk.

This program is not intended for general use to visualise data - another program will be created for users to view 1-second data. Evantually the functionality from this program is expected to be subsumed into other programs.


## How to install the compiled product

1. The compiled program will need NASA's CDF package installed before running. The program will run without this, but won't produce CDF data files
2. Copy the compiled 'jar' file to \\bgsmhgpfs\anon_ftp\INTERMAGNET\software\DataCheck1s
