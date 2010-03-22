require "weakling"
require "jruby"

describe Weakling::IdHash do
  before(:each) do
    @id_hash = Weakling::IdHash.new
  end

  it "should hold mappings from id to object" do
    ary = (1..10).to_a.map {Object.new}
    ids = ary.map {|o| @id_hash.add(o)}

    ids.sort.should == ary.map(&:__id__).sort
    @id_hash.to_a.sort.should == ary.map{|obj| [obj.__id__, obj]}.sort
  end

  it "should weakly reference the objects" do
    ary = (1..10).to_a.map {Object.new}
    ids = ary.map {|o| @id_hash.add(o)}
    ary = nil
    
    JRuby.gc

    @id_hash.to_a.should be_empty
  end
end