use strict;
use File::Find;

our $YEAR = get_properties_file("../nbproject/config.properties")->{year};
die "No year in config file" unless $YEAR;

our $BUILD_NUMBER = get_properties_file("../build.number")->{'build.number'};
die "No build number" unless $BUILD_NUMBER;

our $INSTALLER_EXE = "..\\release\\setupbdv${YEAR}j.exe";
die "Can't find installer $INSTALLER_EXE: $!" unless -x $INSTALLER_EXE;

print STDERR "Found installer '$INSTALLER_EXE'.\n";

our $PROGRAMS_DIR = "C:\\Program Files (x86)";
our $INSTALL_DIR = "$PROGRAMS_DIR\\Bridge Designer 20${YEAR} (2nd Edition)";
our $UNINSTALLER_EXE = "$INSTALL_DIR\\uninstall.exe";
if (-x $UNINSTALLER_EXE) {
  print STDERR "Running uninstaller '$UNINSTALLER_EXE' because the test requires no previous installation.";
  system(qq|"$UNINSTALLER_EXE /S"|);
}

print STDERR "Silent install...\n";
system(qq|"$INSTALLER_EXE /S"|);

my $files = get_file_tree($INSTALL_DIR);
my $n_files = scalar(@$files);

print STDERR "Record installation has $n_files files\n";

print STDERR "Silent uninstall...\n";
system(qq|"$UNINSTALLER_EXE /S"|);

print STDERR "Normal install on clean machine...\n";
system(qq|"$INSTALLER_EXE"|);

print STDERR "Normal re-install on clean machine. Should be no warnings...\n";
system(qq|"$INSTALLER_EXE"|);

print STDERR "Attempt to install in existing directory should fail...\n";
system(qq|"$INSTALLER_EXE" /D=$PROGRAMS_DIR|);

sub get_properties_file {
  my $fn = shift;
  open F, $fn or die "Can't open properties file '$fn': $!";
  my $properties = {};
  while (<F>) {
    chomp;
    next if /^\s*#/;
    my ($var, $val) = /(\S+)\s*=\s*(\S+(:?\s+\S+)*)/;
    $properties->{$var} = $val if $var;
  }
  close F;
  return $properties;
}

sub get_file_tree {
  my $root = shift;
  my @list;
  find(sub { push @list, $File::Find::name }, $root);
  @list = sort(@list);
  return \@list;
}
