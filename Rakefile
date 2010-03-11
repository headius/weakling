require 'ant'

directory "pkg/classes"

task :clean do
  rm_rf "pkg/classes"
  rm_rf "lib/refqueue.jar"
end

task :compile => "pkg/classes" do |t|
  ant.javac :srcdir => "ext", :destdir => t.prerequisites.first,
    :source => "1.5", :target => "1.5", :debug => true,
    :classpath => "${java.class.path}:${sun.boot.class.path}"
end

task :jar => :compile do
  ant.jar :basedir => "pkg/classes", :destfile => "lib/refqueue.jar", :includes => "**/*.class"
end
 
task :package => :jar
