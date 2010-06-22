#!/bin/sh

#
# Called during SVN commit to build a local change log.
#
# Author:  Gunnar Ekolin
#
# Version: 0.2 2008-01-26 Bug fix, using tmp-file name that was a path.
# Version: 0.1 2008-01-16
#

gc_HOME_DIR=/home/knopflerfish; readonly gc_HOME_DIR

usage() {
cat <<EOF
Usage: $0 [-h] [-catch-up] [-log-dir <LOG_DIR>] [-r <REVISION>] [<REPOSITORY>]
  -h             Print this help text.
  -catch-up      If present the add change log entries starting from
                 the given revision up to the most resent revision.
  -log-dir <LOG_DIR>
                 Specify a directory ro place the generated ChangeLog in.
                 Default: ${gc_HOME_DIR}.
  -r <REVISION>  The revision to add a change log entry for.
                 Default: The most resent (youngest) revision.
  <REPOSITORY>   The local repository path.
                 Default: ${gc_HOME_DIR}/svn/repo.
EOF
}

#
# Setup constant global data
#
setup() {
    gc_SVNLOOK=/usr/bin/svnlook; readonly gc_SVNLOOK
}

parse_args() {
    gc_CATCH_UP=
    gc_CHANGE_LOG_DIR=${gc_HOME_DIR}

    while [ $# -gt 0 ]; do
        case "$1" in
            -h)
                usage
                exit 0
                ;;
            -catch-up)
                gc_CATCH_UP=true
                ;;
            -log-dir)
                shift
                gc_CHANGE_LOG_DIR=$1
                ;;
            -r)
                shift
                gn_REV=$1
                ;;
            -*)
                echo "Unknown option, \"${1}\""
                usage
                exit 1
                ;;
            *)
                break
                ;;
        esac
        shift
    done
    readonly gc_CATCH_UP gc_CHANGE_LOG_DIR

    gc_REPO="${1:-${gc_HOME_DIR}/svn/repo}"; readonly gc_REPO
    if [ -z "${gn_REV}" ]; then
        gn_REV=`$gc_SVNLOOK youngest ${gc_REPO}`
    fi
}

#
# Extract commit data from SVN
# Usage: extract_commit_data
# Where:
#
extract_commit_data() {
    ecd_REV="--revision ${gn_REV}"

    g_AUTHOR=`$gc_SVNLOOK author ${gc_REPO} ${ecd_REV}`
    g_CHANGED=`$gc_SVNLOOK changed ${gc_REPO} ${ecd_REV}`
    g_DATE=`$gc_SVNLOOK date ${gc_REPO} ${ecd_REV}`
    g_LOG=`$gc_SVNLOOK log ${gc_REPO} ${ecd_REV}`
    g_DIRS=`$gc_SVNLOOK dirs-changed ${gc_REPO} ${ecd_REV}`
}

#
# Prepend a record to the specified log
# Usage: prepend_entry_to_log <FILE_NAME>
# Where: <FILE_NAME> The name of the file to prepend to.
#
prepend_entry_to_log() {
    atl_File="${1}"
    atl_TMP_File="/tmp/svnChangeLog.$$"

    cat  > ${atl_TMP_File}  <<EOF
${g_DATE}, revision ${gn_REV}, ${g_AUTHOR}
${g_LOG}
${g_CHANGED}

EOF
    [ -f ${atl_File} ] && cat  ${atl_File} >> ${atl_TMP_File}
    cat  ${atl_TMP_File} > ${atl_File}
    rm -f ${atl_TMP_File}
}

#
# Check if a record shall be prepended or not
# Usage: check_prepend <DIR_PATTERN> <LOG_FILE>
# Where: <DIR_PATTERN> is a grep pattern matched against the
#                      directories with changed contents.
#        <LOG_FILE>    is the path to prepend a commit record to if
#                      the pattern matches.
#
check_prepend() {
    ca_PATTERN="${1}"
    ca_FILE="${2}"

    if echo ${g_DIRS} | grep --quiet --no-messages "${ca_PATTERN}"
    then
        prepend_entry_to_log "${ca_FILE}"
    fi
}


main() {
    extract_commit_data

    check_prepend "knopflerfish.org/trunk/" \
                  ${gc_CHANGE_LOG_DIR}/ChangeLog_trunk
    check_prepend "knopflerfish.org/branches/kf_1_support" \
                  ${gc_CHANGE_LOG_DIR}/ChangeLog_kf_1_support
    check_prepend "knopflerfish.org/branches/kf_2_support" \
                  ${gc_CHANGE_LOG_DIR}/ChangeLog_kf_2_support
}

#
# Build ChangeLogs starting from the given revision up to the current
# Usage: catch_up
# Where:
#
catch_up() {
    nYOUNGEST=`$gc_SVNLOOK youngest ${gc_REPO}`
    while [ $gn_REV -le $nYOUNGEST ]; do
        main
        gn_REV=`expr $gn_REV + 1`
    done
}


setup
parse_args "$@"

if [ -n "${gc_CATCH_UP}" ]; then
    catch_up
else
    main
fi
