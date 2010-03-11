require 'weakref'
require 'refqueue'

class WeakRef::IdHash
  attr_accessor :hash

  def initialize
    @hash = Hash.new
    @queue = WeakRef::RefQueue.new
  end

  class IdWeakRef < WeakRef
    attr_accessor :id
    def initialize(obj, queue)
      super(obj, queue)
      @id = obj.__id__
    end
  end

  def [](id)
    _cleanup
    if wr = @hash[id]
      return wr.__getobj__ rescue nil
    end

    return nil
  end

  def add(object)
    _cleanup
    wr = IdWeakRef.new(object, @queue)

    @hash[wr.id] = wr

    return wr.id
  end

  def _cleanup
    while ref = @queue.poll
      @hash.delete(ref.id)
    end
  end
end
