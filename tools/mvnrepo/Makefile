
REVISIONs = 2.3.3 2.4.0 3.0.0 3.1.0

KF_HOME=/home/knopflerfish
WWW_DIR=${KF_HOME}/www
WWW_PATH=maven2
OUT_DIR=${WWW_DIR}/${WWW_PATH}_$$
INSTALL_DIR=${WWW_DIR}/${WWW_PATH}

ANT	= /usr/bin/ant
AWK     = /usr/bin/awk
CAT     = /bin/cat
CP      = /bin/cp 
GREP    = /bin/grep
LN      = /bin/ln
MKDIR   = /bin/mkdir
MV      = /bin/mv
RM      = /bin/rm
SED     = /bin/sed
SVN     = /usr/bin/svn

#JAVA      = java
JAVA      = /usr/local/j2sdk1.4/bin/java
export JAVA_HOME = /usr/local/j2sdk1.4

# Ant pre-configured for generating maven 2 repository (JSE 5 or higher required)
TAGsABS=$(shell /bin/pwd)/${TAGs}
ANTmvn = env JAVA_HOME=/usr ${ANT} \
         -Dmvn2.repo=${OUT_DIR}

.PHONY: help all clean
.PHONY: mvnrepo

help:
	@echo "mvnrepo      Builds the global maven 2 repo"
	@echo "install      Replaces the old repo with the one just built"

all: mvnrepo install

mvnrepo:
	[ -r ${TAGs}/out/tmp/toMvn.xml ] && (cd ${TAGs} && ${ANTmvn} -f out/tmp/toMvn.xml)

install:
	$(RM) -rf ${INSTALL_DIR}
	$(MV) ${OUT_DIR} ${INSTALL_DIR}

clean:
	$(RM) -rf ${OUT_DIR}

