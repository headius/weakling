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
puts "hash size: #{wh.hash.size}"
