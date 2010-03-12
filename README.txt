weakling: a collection of weakref utilities for Ruby

== Summary ==

This library provides:

* a modified WeakRef implementation for JRuby that supports a reference queue
* a WeakRef::RefQueue class
* a weak-valued ID map to replace typical uses of _id2ref

In the future it may provide additional features like a generic WeakHash or other
reference types like soft and phantom references.

== Usage ==

Just require 'weakling'. It will require 'weakref' along with the refqueue JRuby
extension and the weakling/collections library containing the weak id hash.

== Example ==

require 'weakling'
 
wh = WeakRef::IdHash.new
 
ary = (1..10).to_a.map {Object.new}
ids = ary.map {|o| wh.add(o)}
 
puts "all items in weak_id_hash:"
ids.each {|i| puts "#{i} = #{wh[i]}"}
 
puts "dereferencing objects"
ary = nil
 
puts "forcing GC"
begin
  require 'java'
  java.lang.System.gc
rescue
  GC.start
end
 
puts "all items in weak id hash:"
ids.each {|i| puts "#{i} = #{wh[i]}"}
